package com.cpclub.backend.codeforces.client;

import com.cpclub.backend.codeforces.dto.CfContest;
import com.cpclub.backend.codeforces.dto.CfProblem;
import com.cpclub.backend.codeforces.dto.CfProblemStatistics;
import com.cpclub.backend.codeforces.dto.CfRatingChange;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;

/**
 * Anonymous client for the public Codeforces API.
 *
 * <p><b>Jackson 3 only.</b> Spring Boot 4 serializes with Jackson 3
 * ({@code tools.jackson}); Jackson 2 ({@code com.fasterxml.jackson.databind}) is
 * only on the classpath transitively. This client used to ask RestTemplate for
 * Jackson 2 {@code JsonNode}s, which the Jackson 3 converter cannot construct, so
 * every call whose result held at least one object failed with
 * "Type definition error: [simple type, class com.fasterxml.jackson.databind.JsonNode]".
 * A member with no rated contests got an empty list back and "succeeded", which is
 * why the contest-history sync reported roughly half the club as failing.
 * Responses are now read as a stream and bound to typed DTOs by a Jackson 3 mapper
 * owned by this class.</p>
 *
 * <p>Every request acquires a permit from the shared Codeforces limiter (D23) and
 * goes through the application {@link RestTemplate}, which has connect and read
 * timeouts. A bare {@code new RestTemplate()} has none, and one half-open
 * connection would block the scheduler thread for good.</p>
 */
@Slf4j
@Component
public class CodeforcesApiClient {

    private static final String API_BASE = "https://codeforces.com/api/";

    /** One original attempt plus two retries for 5xx, network errors and throttling. */
    private static final int MAX_ATTEMPTS = 3;

    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;
    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public CodeforcesApiClient(RestTemplate restTemplate,
                               @Qualifier("codeforcesRateLimiter") RateLimiter rateLimiter) {
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CfResponse<T> {
        private String status;
        private String comment;
        private T result;
    }

    /** The handle does not exist on Codeforces: renamed, deleted, or mistyped. */
    public static class HandleNotFoundException extends RuntimeException {
        public HandleNotFoundException(String message) {
            super(message);
        }
    }

    /** Any other Codeforces failure, with a message safe to show in Sync Health. */
    public static class CodeforcesApiException extends RuntimeException {
        public CodeforcesApiException(String message) {
            super(message);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProblemsetResult {
        private List<CfProblem> problems;
        private List<CfProblemStatistics> problemStatistics;
    }

    public List<CfSubmission> userStatus(String handle, int from, int count) {
        URI uri = UriComponentsBuilder.fromUriString(API_BASE + "user.status")
                .queryParam("handle", handle)
                .queryParam("from", from)
                .queryParam("count", count)
                .encode()
                .build()
                .toUri();
        return call(uri, new TypeReference<CfResponse<List<CfSubmission>>>() {});
    }

    public List<CfRatingChange> userRating(String handle) {
        URI uri = UriComponentsBuilder.fromUriString(API_BASE + "user.rating")
                .queryParam("handle", handle)
                .encode()
                .build()
                .toUri();
        return call(uri, new TypeReference<CfResponse<List<CfRatingChange>>>() {});
    }

    public List<CfContest> contestList(boolean gym) {
        URI uri = UriComponentsBuilder.fromUriString(API_BASE + "contest.list")
                .queryParam("gym", gym)
                .build()
                .toUri();
        return call(uri, new TypeReference<CfResponse<List<CfContest>>>() {});
    }

    public ProblemsetResult problemsetProblems() {
        URI uri = URI.create(API_BASE + "problemset.problems");
        return call(uri, new TypeReference<CfResponse<ProblemsetResult>>() {});
    }

    private <T> T call(URI uri, TypeReference<CfResponse<T>> type) {
        String path = uri.getPath();
        for (int attempt = 1; ; attempt++) {
            rateLimiter.acquire();
            CfResponse<T> body;
            try {
                // Streamed straight into the DTOs: problemset.problems is several MB,
                // and holding it as a String first would double the peak on a 160 MB heap.
                body = restTemplate.execute(uri, HttpMethod.GET, null,
                        response -> json.readValue(response.getBody(), type));
            } catch (HttpStatusCodeException e) {
                // Codeforces answers a bad handle with 400 and a normal JSON envelope.
                body = parseErrorBody(e.getResponseBodyAsString(), type);
                if (body == null) {
                    if (e.getStatusCode().is5xxServerError() && attempt < MAX_ATTEMPTS) {
                        log.warn("Codeforces {} returned HTTP {}, retrying ({}/{})",
                                path, e.getStatusCode().value(), attempt, MAX_ATTEMPTS);
                        continue;
                    }
                    throw new CodeforcesApiException("Codeforces " + path + " returned HTTP " + e.getStatusCode().value());
                }
            } catch (ResourceAccessException e) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("Codeforces {} unreachable ({}), retrying ({}/{})", path, e.getMessage(), attempt, MAX_ATTEMPTS);
                    continue;
                }
                throw new CodeforcesApiException("Codeforces " + path + " unreachable: " + e.getMessage());
            } catch (JacksonException e) {
                throw new CodeforcesApiException("Codeforces " + path + " sent an unreadable response: " + e.getOriginalMessage());
            }

            if (body == null) {
                throw new CodeforcesApiException("Empty response from Codeforces " + path);
            }
            if ("OK".equals(body.getStatus())) {
                return body.getResult();
            }

            String comment = body.getComment() == null ? "no comment" : body.getComment();
            if (isThrottle(comment) && attempt < MAX_ATTEMPTS) {
                log.warn("Codeforces throttled {} ({}), retrying ({}/{})", path, comment, attempt, MAX_ATTEMPTS);
                continue;
            }
            if (comment.contains("not found")) {
                throw new HandleNotFoundException(comment);
            }
            throw new CodeforcesApiException("Codeforces API returned FAILED: " + comment);
        }
    }

    private <T> CfResponse<T> parseErrorBody(String raw, TypeReference<CfResponse<T>> type) {
        if (raw == null || raw.isBlank() || !raw.trim().startsWith("{")) {
            return null;
        }
        try {
            return json.readValue(raw, type);
        } catch (JacksonException e) {
            return null;
        }
    }

    private static boolean isThrottle(String comment) {
        String lower = comment.toLowerCase();
        return lower.contains("call limit") || lower.contains("too many");
    }
}
