package com.cpclub.backend.contest.client;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads club contest standings from Codeforces.
 *
 * <p>There are two routes, and which one applies is decided by Codeforces, not by
 * this class.</p>
 *
 * <p><b>Gym contests</b> -- every GYM, MASHUP and GROUP contest, all of which have an
 * id of 100000 or more -- require an authenticated request even when the gym is
 * public. Requests are signed with the club's API key (D10) and filtered to the club's
 * handles on the Codeforces side, so the response is small.</p>
 *
 * <p><b>Official contests</b> answer only an anonymous request with no parameters at
 * all; Codeforces rejects {@code handles}, {@code from}, {@code count} and
 * {@code showUnofficial} for non-gym standings, signed or not. That request returns the
 * entire ranklist -- measured at 7 MB for a Div. 4 round and 14 MB for a Div. 2. On a
 * 160 MB heap (amendment A4), deserialising that into objects is exactly the failure
 * mode the amendment warns about. So the response is streamed: rows are read one at a
 * time and only those containing a club member's handle are kept, and when only the
 * header is wanted the stream is abandoned as soon as the problem list has been read.
 * Peak memory is one row plus the club's own results, whatever the size of the round.</p>
 *
 * <p>Side effect of the anonymous route: it returns official participants only, so a
 * member who entered an official round out of competition (a Div. 1 member in a Div. 2
 * round, say) does not appear in the club ranklist. Codeforces offers no way round
 * that without contest-admin rights.</p>
 *
 * <p><b>Nothing secret reaches a log or an exception message.</b> The API key and the
 * signature ride in the query string, and Spring's own client exceptions quote the
 * full request URL in their message. Every string this class logs or throws passes
 * through {@link #redact}, and the original exception is never chained, so a stack
 * trace cannot carry the URL either.</p>
 */
@Slf4j
@Component
public class CodeforcesAuthorizedClient {

    static final String API_BASE = "https://codeforces.com/api/";

    /** Codeforces numbers every gym contest from 100000 upwards. */
    public static final int GYM_ID_FLOOR = 100_000;

    private static final Pattern SECRET_PARAMS = Pattern.compile("(apiKey|apiSig)=[^&\\s\"']*");
    private static final Pattern MISSING_HANDLE = Pattern.compile("User with handle (\\S+) not found");
    /** Bad handles dropped from one standings call before giving up; each costs one request. */
    private static final int MAX_DROPPED_HANDLES = 25;
    /** How long a handle Codeforces reported missing is left out of standings calls. */
    private static final Duration MISSING_HANDLE_TTL = Duration.ofHours(24);
    private static final String ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;
    private final String apiKey;
    private final String apiSecret;
    private final Clock clock;
    private final Supplier<String> randSource;
    /**
     * Handles Codeforces said do not exist, lowercased, with when it said so. Without
     * this, every sync re-learned each bad handle at the cost of one rate-limited
     * request apiece, which pushed a manual sync past the browser's timeout.
     */
    private final Map<String, Instant> knownMissing = new ConcurrentHashMap<>();
    /**
     * Jackson 3 turns on FAIL_ON_TRAILING_TOKENS by default, which treats everything
     * after the first value as an error. Reading one row at a time from a stream is
     * precisely "a value followed by more tokens", so it is off here.
     */
    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    @Autowired
    public CodeforcesAuthorizedClient(
            RestTemplate restTemplate,
            // The same host as the rest of the Codeforces traffic, so the same budget (D23).
            @Qualifier("codeforcesRateLimiter") RateLimiter rateLimiter,
            @Value("${cpclub.codeforces.api-key:}") String apiKey,
            @Value("${cpclub.codeforces.api-secret:}") String apiSecret,
            Clock clock) {
        this(restTemplate, rateLimiter, apiKey, apiSecret, clock, CodeforcesAuthorizedClient::randomRand);
    }

    /** Lets tests fix {@code rand}, which is otherwise the one unpredictable input to the signature. */
    CodeforcesAuthorizedClient(RestTemplate restTemplate, RateLimiter rateLimiter, String apiKey,
                               String apiSecret, Clock clock, Supplier<String> randSource) {
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();
        this.clock = clock;
        this.randSource = randSource;
    }

    /** Whether gym, mashup and group contests can be read at all. */
    public boolean isConfigured() {
        return !apiKey.isEmpty() && !apiSecret.isEmpty();
    }

    public static boolean isGym(int cfContestId) {
        return cfContestId >= GYM_ID_FLOOR;
    }

    /**
     * The contest and its problems, without any rows. Used to validate a contest before
     * it is added. For an official round only the head of the response is read.
     */
    public CfStandings contestHeader(int cfContestId) {
        return fetch(cfContestId, List.of(), true);
    }

    /**
     * The contest, its problems, and the rows belonging to any of {@code handles},
     * matched case-insensitively. A team row is kept when any one of its members is a
     * club member.
     */
    public CfStandings contestStandings(int cfContestId, Collection<String> handles) {
        return fetch(cfContestId, handles, false);
    }

    private CfStandings fetch(int cfContestId, Collection<String> handles, boolean headerOnly) {
        List<String> asStored = handles.stream()
                .filter(h -> h != null && !h.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        // Matching is case-insensitive on the club's side. The handles are sent to
        // Codeforces as members stored them, since its filter is not documented as
        // case-insensitive and lowercasing could make it miss a row outright.
        Set<String> wanted = asStored.stream()
                .map(h -> h.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (!isGym(cfContestId)) {
            return execute(URI.create(API_BASE + "contest.standings?contestId=" + cfContestId), wanted, headerOnly);
        }
        if (!isConfigured()) {
            throw new CodeforcesApiException("Codeforces API key not configured");
        }

        // Codeforces fails the whole call when any one handle in the filter does not
        // exist, and names it: "handles: User with handle X not found". One member
        // with a mistyped or renamed handle used to make every sync of every gym
        // contest fail. That handle is dropped and the call re-signed and repeated.
        Instant now = clock.instant();
        knownMissing.entrySet().removeIf(e -> e.getValue().plus(MISSING_HANDLE_TTL).isBefore(now));
        List<String> sendable = asStored.stream()
                .filter(h -> !knownMissing.containsKey(h.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(ArrayList::new));
        for (int dropped = 0; ; dropped++) {
            try {
                return execute(gymStandingsUri(cfContestId, sendable, headerOnly), wanted, headerOnly);
            } catch (CodeforcesApiException e) {
                Matcher missing = MISSING_HANDLE.matcher(String.valueOf(e.getMessage()));
                if (headerOnly || dropped >= MAX_DROPPED_HANDLES || !missing.find()) {
                    throw e;
                }
                String bad = missing.group(1);
                if (!sendable.removeIf(h -> h.equalsIgnoreCase(bad))) {
                    throw e;
                }
                knownMissing.put(bad.toLowerCase(Locale.ROOT), clock.instant());
                log.warn("Codeforces has no user '{}'; leaving them out of contest {} standings", bad, cfContestId);
            }
        }
    }

    private URI gymStandingsUri(int cfContestId, List<String> handles, boolean headerOnly) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("contestId", Integer.toString(cfContestId));
        if (headerOnly) {
            // Only the contest and its problems are wanted; one row keeps the reply tiny.
            params.put("from", "1");
            params.put("count", "1");
        } else if (!handles.isEmpty()) {
            // Codeforces filters on its side: one row per club member at most.
            params.put("handles", String.join(";", handles));
        }
        // Out-of-competition entries are unofficial on Codeforces; without this they
        // are dropped before the club's own filter ever sees them.
        params.put("showUnofficial", "true");
        return signedUri("contest.standings", params);
    }

    private CfStandings execute(URI uri, Set<String> wanted, boolean headerOnly) {
        RestClientException lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            rateLimiter.acquire();
            try {
                return restTemplate.execute(uri, HttpMethod.GET, null,
                        response -> parse(response.getBody(), wanted, headerOnly));
            } catch (HttpStatusCodeException e) {
                // Codeforces answers a FAILED call with a 4xx and a JSON body naming the
                // problem. That comment is safe to surface; the URL is not.
                // Redacted anyway: an error message is the one place Codeforces could
                // echo a request parameter back.
                String comment = redact(failedComment(e.getResponseBodyAsString()));
                if (e.getStatusCode().is4xxClientError()) {
                    throw new CodeforcesApiException(comment != null ? comment
                            : "Codeforces rejected the request (" + e.getStatusCode().value() + ")");
                }
                lastFailure = e;
            } catch (CodeforcesApiException e) {
                throw e;
            } catch (RestClientException e) {
                lastFailure = e;
            }
            log.warn("Codeforces standings request failed (attempt {}): {} {}", attempt,
                    lastFailure.getClass().getSimpleName(), redact(uri.toString()));
        }
        throw new CodeforcesApiException("Codeforces standings unavailable: "
                + lastFailure.getClass().getSimpleName() + " " + redact(String.valueOf(lastFailure.getMessage())));
    }

    // ---------------------------------------------------------------- parsing

    /** Package-private so the parser can be tested against a captured response. */
    CfStandings parse(InputStream body, Set<String> wanted, boolean headerOnly) throws IOException {
        CfStandings standings = new CfStandings();
        String status = null;
        String comment = null;
        try (JsonParser p = json.createParser(body)) {
            expect(p.nextToken(), JsonToken.START_OBJECT);
            while (p.nextToken() == JsonToken.PROPERTY_NAME) {
                String name = p.currentName();
                p.nextToken();
                switch (name) {
                    case "status" -> status = p.getValueAsString();
                    case "comment" -> comment = p.getValueAsString();
                    case "result" -> {
                        if (readResult(p, standings, wanted, headerOnly)) {
                            // Header-only read finished early: stop without draining the
                            // rest of a multi-megabyte body.
                            return standings;
                        }
                    }
                    default -> p.skipChildren();
                }
            }
        } catch (JacksonException e) {
            // The parser describes the body, never the request, so its message is safe to log.
            log.warn("Could not parse a Codeforces standings response: {}", e.getOriginalMessage());
            throw new CodeforcesApiException("Unreadable standings response from Codeforces");
        }
        if (!"OK".equals(status)) {
            throw new CodeforcesApiException(comment != null ? redact(comment) : "Codeforces returned status " + status);
        }
        return standings;
    }

    /** Returns true when a header-only read has everything it needs and can stop. */
    private boolean readResult(JsonParser p, CfStandings standings, Set<String> wanted, boolean headerOnly) {
        expect(p.currentToken(), JsonToken.START_OBJECT);
        boolean haveContest = false;
        boolean haveProblems = false;
        while (p.nextToken() == JsonToken.PROPERTY_NAME) {
            String name = p.currentName();
            p.nextToken();
            switch (name) {
                case "contest" -> {
                    standings.setContest(json.readValue(p, CfStandings.Contest.class));
                    haveContest = true;
                }
                case "problems" -> {
                    expect(p.currentToken(), JsonToken.START_ARRAY);
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        standings.getProblems().add(json.readValue(p, CfStandings.Problem.class));
                    }
                    haveProblems = true;
                }
                case "rows" -> {
                    if (headerOnly) {
                        return true;
                    }
                    expect(p.currentToken(), JsonToken.START_ARRAY);
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        // One row in memory at a time; kept only when it is the club's.
                        CfStandings.Row row = json.readValue(p, CfStandings.Row.class);
                        if (belongsToClub(row, wanted)) {
                            standings.getRows().add(row);
                        }
                    }
                }
                default -> p.skipChildren();
            }
            if (headerOnly && haveContest && haveProblems) {
                return true;
            }
        }
        return false;
    }

    private static boolean belongsToClub(CfStandings.Row row, Set<String> wanted) {
        if (wanted.isEmpty() || row.getParty() == null) {
            return false;
        }
        for (CfStandings.Member m : row.getParty().getMembers()) {
            if (m.getHandle() != null && wanted.contains(m.getHandle().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static void expect(JsonToken actual, JsonToken expected) {
        if (actual != expected) {
            throw new CodeforcesApiException("Unexpected standings response shape from Codeforces");
        }
    }

    private String failedComment(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            var node = json.readTree(body);
            var c = node.get("comment");
            return c == null ? null : c.asString();
        } catch (JacksonException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- signing

    private URI signedUri(String method, Map<String, String> params) {
        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("apiKey", apiKey);
        all.put("time", Long.toString(clock.instant().getEpochSecond()));
        String rand = randSource.get();
        all.put("apiSig", apiSig(method, all, rand));
        String query = all.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return URI.create(API_BASE + method + "?" + query);
    }

    /**
     * {@code rand + sha512Hex(rand + "/" + method + "?" + sortedParams + "#" + secret)}.
     *
     * <p>Parameters are sorted by name and then by value, and joined with their raw
     * values -- the signature covers what the server decodes, not the URL encoding.
     * {@code apiSig} itself is excluded if already present.</p>
     */
    String apiSig(String method, Map<String, String> params, String rand) {
        String sorted = params.entrySet().stream()
                .filter(e -> !"apiSig".equals(e.getKey()))
                .sorted(Map.Entry.<String, String>comparingByKey()
                        .thenComparing(Map.Entry.comparingByValue()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        String input = rand + "/" + method + "?" + sorted + "#" + apiSecret;
        return rand + sha512Hex(input);
    }

    static String sha512Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 unavailable", e);
        }
    }

    private static String randomRand() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALNUM.charAt(RANDOM.nextInt(ALNUM.length())));
        }
        return sb.toString();
    }

    /** Strips the key and signature from anything that might be logged or thrown. */
    String redact(String text) {
        if (text == null) {
            return null;
        }
        String out = SECRET_PARAMS.matcher(text).replaceAll("$1=[redacted]");
        // Belt and braces: the raw values, in case they surface outside a query string.
        if (!apiKey.isEmpty()) {
            out = out.replace(apiKey, "[redacted]");
        }
        if (!apiSecret.isEmpty()) {
            out = out.replace(apiSecret, "[redacted]");
        }
        return out;
    }

    /** A Codeforces failure, with a message that is safe to show an admin. */
    public static class CodeforcesApiException extends RuntimeException {
        public CodeforcesApiException(String message) {
            super(message);
        }
    }
}
