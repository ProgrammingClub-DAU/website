package com.cpclub.backend.contest.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient.CodeforcesApiException;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The signed Codeforces client.
 *
 * <p>Signature tests recompute the expected value by hand from the documented
 * formula rather than calling back into the class, so a bug in the implementation
 * cannot also be a bug in the expectation.</p>
 */
class CodeforcesAuthorizedClientTest {

    private static final String KEY = "club-key-0123456789abcdef";
    private static final String SECRET = "club-secret-fedcba9876543210";
    private static final Clock FIXED = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);

    private RestTemplate restTemplate;
    private CodeforcesAuthorizedClient client;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = newClient(KEY, SECRET);

        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(CodeforcesAuthorizedClient.class)).addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(CodeforcesAuthorizedClient.class)).detachAppender(logs);
    }

    @Test
    @DisplayName("apiSig matches the documented formula, computed independently")
    void apiSig_matchesTheDocumentedFormula() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("contestId", "102345");
        params.put("apiKey", KEY);
        params.put("time", "1700000000");

        String sig = apiSig("contest.standings", params, "abc123");

        String input = "abc123/contest.standings?apiKey=" + KEY
                + "&contestId=102345&time=1700000000#" + SECRET;
        assertEquals("abc123" + sha512(input), sig);
    }

    @Test
    @DisplayName("Parameters given out of order are signed sorted by name, then value")
    void apiSig_sortsParametersByNameThenValue() throws Exception {
        Map<String, String> scrambled = new LinkedHashMap<>();
        scrambled.put("time", "1700000000");
        scrambled.put("showUnofficial", "true");
        scrambled.put("handles", "Alice;bob");
        scrambled.put("apiKey", KEY);
        scrambled.put("contestId", "102345");

        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("apiKey", KEY);
        ordered.put("contestId", "102345");
        ordered.put("handles", "Alice;bob");
        ordered.put("showUnofficial", "true");
        ordered.put("time", "1700000000");

        assertEquals(apiSig("contest.standings", ordered, "zZ9yY8"),
                apiSig("contest.standings", scrambled, "zZ9yY8"));

        // Values are signed raw: the ';' in handles is not URL-encoded in the input.
        String input = "zZ9yY8/contest.standings?apiKey=" + KEY
                + "&contestId=102345&handles=Alice;bob&showUnofficial=true&time=1700000000#" + SECRET;
        assertEquals("zZ9yY8" + sha512(input), apiSig("contest.standings", scrambled, "zZ9yY8"));
    }

    @Test
    @DisplayName("A failed request logs and throws nothing secret")
    void failure_neverLeaksTheKeySecretOrSignature() {
        // Spring's I/O exception quotes the whole request URL, signature and all.
        when(restTemplate.execute(any(URI.class), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class)))
                .thenAnswer(inv -> {
                    throw new ResourceAccessException("I/O error on GET request for \""
                            + inv.getArgument(0) + "\": Connect timed out");
                });

        CodeforcesApiException thrown = assertThrows(CodeforcesApiException.class,
                () -> client.contestStandings(102345, List.of("alice")));

        StringBuilder everything = new StringBuilder(thrown.getMessage()).append('\n');
        for (ILoggingEvent e : logs.list) {
            everything.append(e.getFormattedMessage()).append('\n');
        }
        String text = everything.toString();

        assertFalse(logs.list.isEmpty(), "the failure should have been logged");
        assertFalse(text.contains(KEY), "API key leaked:\n" + text);
        assertFalse(text.contains(SECRET), "API secret leaked:\n" + text);
        assertFalse(text.matches("(?s).*apiSig=[0-9A-Za-z]{6}[0-9a-f]{128}.*"), "signature leaked:\n" + text);
        assertTrue(text.contains("apiSig=[redacted]"), "expected the URL with the signature redacted:\n" + text);
        // The original exception carries the raw URL, so it must not be chained.
        assertEquals(null, thrown.getCause());
    }

    @Test
    @DisplayName("Gym contests cannot be read without a key")
    void gymWithoutKey_isRefused() {
        CodeforcesAuthorizedClient unkeyed = newClient("", "");

        CodeforcesApiException e = assertThrows(CodeforcesApiException.class,
                () -> unkeyed.contestHeader(102345));

        assertEquals("Codeforces API key not configured", e.getMessage());
    }

    @Test
    @DisplayName("Official rounds are requested anonymously, with no parameters but the id")
    void officialContest_isRequestedWithoutExtraParameters() throws Exception {
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        when(restTemplate.execute(uri.capture(), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class)))
                .thenAnswer(inv -> extract(inv.getArgument(3), fixture()));

        client.contestStandings(1352, List.of("someone"));

        // Codeforces rejects anything else for non-gym standings, signed or not.
        assertEquals("https://codeforces.com/api/contest.standings?contestId=1352", uri.getValue().toString());
    }

    @Test
    @DisplayName("Gym contests are signed and filtered to the club's handles on Codeforces' side")
    void gymContest_isSignedAndFiltered() {
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        when(restTemplate.execute(uri.capture(), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class)))
                .thenAnswer(inv -> extract(inv.getArgument(3), fixture()));

        client.contestStandings(102345, List.of("Alice", "bob"));

        String sent = uri.getValue().toString();
        assertTrue(sent.contains("apiKey=" + KEY));
        assertTrue(sent.contains("time=1700000000"));
        assertTrue(sent.contains("apiSig="));
        assertTrue(sent.contains("showUnofficial=true"));
        // Sent as stored, not lowercased: Codeforces' filter is not documented as case-insensitive.
        assertTrue(sent.contains("handles=Alice%3Bbob"), sent);
    }

    /**
     * Codeforces fails a filtered standings call outright when any one handle in it
     * does not exist. With a single mistyped member handle, every gym contest sync
     * failed with "Failed to sync standings".
     */
    @Test
    @DisplayName("An unknown handle in the filter is dropped and the call repeated, re-signed")
    void gymContest_unknownHandleIsDroppedAndRetried() {
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        when(restTemplate.execute(uri.capture(), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class)))
                .thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request",
                        org.springframework.http.HttpHeaders.EMPTY,
                        "{\"status\":\"FAILED\",\"comment\":\"handles: User with handle ghost_handle not found\"}"
                                .getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8))
                .thenAnswer(inv -> extract(inv.getArgument(3), fixture()));

        CfStandings standings = client.contestStandings(102345, List.of("Alice", "Ghost_Handle", "bob"));

        List<URI> sent = uri.getAllValues();
        assertEquals(2, sent.size());
        assertTrue(sent.get(0).toString().contains("Ghost_Handle"), sent.get(0).toString());
        assertTrue(sent.get(1).toString().contains("handles=Alice%3Bbob"), sent.get(1).toString());
        assertFalse(sent.get(1).toString().contains("Ghost"));
        assertTrue(standings.getContest() != null);
    }

    @Test
    @DisplayName("A handle reported missing is left out of later syncs from the start")
    void gymContest_knownMissingHandleIsSkippedNextTime() {
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        when(restTemplate.execute(uri.capture(), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class)))
                .thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request",
                        org.springframework.http.HttpHeaders.EMPTY,
                        "{\"status\":\"FAILED\",\"comment\":\"handles: User with handle ghost_handle not found\"}"
                                .getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8))
                .thenAnswer(inv -> extract(inv.getArgument(3), fixture()));

        client.contestStandings(102345, List.of("Alice", "ghost_handle"));
        client.contestStandings(102345, List.of("Alice", "Ghost_Handle"));

        // First sync: failed call + retry. Second sync: one call, ghost never sent.
        List<URI> sent = uri.getAllValues();
        assertEquals(3, sent.size());
        assertFalse(sent.get(2).toString().toLowerCase().contains("ghost"), sent.get(2).toString());
    }

    @Test
    @DisplayName("A failure that names no handle is not retried")
    void gymContest_otherFailuresAreNotRetried() {
        when(restTemplate.execute(any(URI.class), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class)))
                .thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request",
                        org.springframework.http.HttpHeaders.EMPTY,
                        "{\"status\":\"FAILED\",\"comment\":\"contestId: You have no access to the contest\"}"
                                .getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8));

        CodeforcesApiException e = assertThrows(CodeforcesApiException.class,
                () -> client.contestStandings(102345, List.of("Alice")));
        assertTrue(e.getMessage().contains("no access"));
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(1))
                .execute(any(URI.class), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class));
    }

    @Test
    @DisplayName("The captured real response parses, and rows are matched case-insensitively")
    void parsesTheCapturedResponse() throws Exception {
        CfStandings header = parse(fixture(), Set.of(), true);
        assertEquals(1352, header.getContest().getId());
        assertEquals("ICPC", header.getContest().getType());
        assertEquals(7200L, header.getContest().getDurationSeconds());
        assertEquals(7, header.getProblems().size());
        assertEquals("A", header.getProblems().get(0).getIndex());
        assertEquals(800, header.getProblems().get(0).getRating());
        assertTrue(header.getRows().isEmpty(), "a header read keeps no rows");

        // The first row's handle is Loyal_Omkaryote; ask for it in another case.
        CfStandings rows = parse(fixture(), Set.of("loyal_omkaryote"), false);
        assertEquals(1, rows.getRows().size());
        CfStandings.Row row = rows.getRows().get(0);
        assertEquals("CONTESTANT", row.getParty().getParticipantType());
        assertEquals(158, row.getPenalty());
        assertTrue(row.getProblemResults().get(0).isSolved());
    }

    @Test
    @DisplayName("A ranklist of tens of thousands of rows keeps only the club's")
    void streamsALargeRanklistKeepingOnlyClubRows() throws Exception {
        StringBuilder sb = new StringBuilder("{\"status\":\"OK\",\"result\":{\"contest\":{\"id\":2000,"
                + "\"name\":\"Div. 2\",\"type\":\"CF\",\"durationSeconds\":7200,\"startTimeSeconds\":1700000000},"
                + "\"problems\":[{\"contestId\":2000,\"index\":\"A\",\"name\":\"A\",\"rating\":800}],\"rows\":[");
        int total = 30_000;
        for (int i = 0; i < total; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String handle = (i == 12_345 || i == 29_999) ? "ClubMember" + i : "outsider" + i;
            sb.append("{\"party\":{\"contestId\":2000,\"members\":[{\"handle\":\"").append(handle)
                    .append("\"}],\"participantType\":\"CONTESTANT\"},\"rank\":").append(i + 1)
                    .append(",\"points\":10.0,\"penalty\":0,\"problemResults\":[{\"points\":1.0,"
                            + "\"rejectedAttemptCount\":0,\"type\":\"FINAL\"}]}");
        }
        sb.append("]}}");

        CfStandings s = parse(sb.toString().getBytes(StandardCharsets.UTF_8),
                Set.of("clubmember12345", "clubmember29999"), false);

        assertEquals(2, s.getRows().size());
        assertEquals(12_346, s.getRows().get(0).getRank());
        assertEquals(30_000, s.getRows().get(1).getRank());
    }

    @Test
    @DisplayName("A FAILED response surfaces Codeforces' own comment")
    void failedStatus_surfacesTheComment() {
        byte[] body = "{\"status\":\"FAILED\",\"comment\":\"contestId: Contest with id 999999 not found\"}"
                .getBytes(StandardCharsets.UTF_8);

        CodeforcesApiException e = assertThrows(CodeforcesApiException.class, () -> parse(body, Set.of(), true));

        assertEquals("contestId: Contest with id 999999 not found", e.getMessage());
    }

    // ------------------------------------------------------------------ helpers

    private CodeforcesAuthorizedClient newClient(String key, String secret) {
        return new CodeforcesAuthorizedClient(restTemplate, RateLimiter.create(1000), key, secret,
                FIXED, () -> "abc123");
    }

    private String apiSig(String method, Map<String, String> params, String rand) {
        return client.apiSig(method, params, rand);
    }

    private CfStandings parse(byte[] body, Set<String> wanted, boolean headerOnly) throws Exception {
        return client.parse(new ByteArrayInputStream(body), wanted, headerOnly);
    }

    private static Object extract(ResponseExtractor<?> extractor, byte[] body) throws Exception {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(body));
        return extractor.extractData(response);
    }

    private static byte[] fixture() throws Exception {
        try (InputStream in = CodeforcesAuthorizedClientTest.class
                .getResourceAsStream("/fixtures/cf-contest-standings.json")) {
            return in.readAllBytes();
        }
    }

    private static String sha512(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512")
                .digest(s.getBytes(StandardCharsets.UTF_8)));
    }
}
