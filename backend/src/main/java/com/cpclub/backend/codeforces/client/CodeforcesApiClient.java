package com.cpclub.backend.codeforces.client;

import com.cpclub.backend.codeforces.dto.CfProblem;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.codeforces.dto.CfUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class CodeforcesApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RateLimiter rateLimiter;

    public CodeforcesApiClient(@Qualifier("codeforcesRateLimiter") RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CfResponse<T> {
        private String status;
        private String comment;
        private T result;
    }

    public static class HandleNotFoundException extends RuntimeException {
        public HandleNotFoundException(String message) {
            super(message);
        }
    }

    private <T> T get(String url, ParameterizedTypeReference<CfResponse<T>> responseType) {
        rateLimiter.acquire();
        try {
            return executeWithRetry(url, responseType);
        } catch (RestClientException e) {
            log.warn("Codeforces API call failed, retrying once: {}", url);
            rateLimiter.acquire(); // Fresh permit for retry
            return executeWithRetry(url, responseType);
        }
    }

    private <T> T executeWithRetry(String url, ParameterizedTypeReference<CfResponse<T>> responseType) {
        try {
            ResponseEntity<CfResponse<T>> response = restTemplate.exchange(url, HttpMethod.GET, null, responseType);
            CfResponse<T> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from Codeforces API");
            }
            if ("FAILED".equals(body.getStatus())) {
                if (body.getComment() != null && body.getComment().contains("not found")) {
                    throw new HandleNotFoundException(body.getComment());
                }
                throw new RuntimeException("Codeforces API returned FAILED: " + body.getComment());
            }
            return body.getResult();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            if (e.getStatusCode().is4xxClientError()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    CfResponse<?> errorBody = mapper.readValue(e.getResponseBodyAsString(), CfResponse.class);
                    if ("FAILED".equals(errorBody.getStatus())) {
                        if (errorBody.getComment() != null && errorBody.getComment().contains("not found")) {
                            throw new HandleNotFoundException(errorBody.getComment());
                        }
                        throw new RuntimeException("Codeforces API returned FAILED: " + errorBody.getComment());
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException parseEx) {
                    // Ignore parsing error and rethrow the HTTP exception
                }
            }
            throw e;
        }
    }

    public List<CfSubmission> userStatus(String handle, int from, int count) {
        String url = String.format("https://codeforces.com/api/user.status?handle=%s&from=%d&count=%d", handle, from, count);
        return get(url, new ParameterizedTypeReference<>() {});
    }

    public List<JsonNode> userRating(String handle) {
        String url = String.format("https://codeforces.com/api/user.rating?handle=%s", handle);
        return get(url, new ParameterizedTypeReference<>() {});
    }

    public List<JsonNode> contestList(boolean gym) {
        String url = String.format("https://codeforces.com/api/contest.list?gym=%b", gym);
        return get(url, new ParameterizedTypeReference<>() {});
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProblemsetResult {
        private List<CfProblem> problems;
        private List<JsonNode> problemStatistics;
    }

    public ProblemsetResult problemsetProblems() {
        String url = "https://codeforces.com/api/problemset.problems";
        return get(url, new ParameterizedTypeReference<>() {});
    }
}
