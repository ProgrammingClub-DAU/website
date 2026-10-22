package com.cpclub.backend.codeforces.client;

import com.cpclub.backend.codeforces.dto.CfContest;
import com.cpclub.backend.codeforces.dto.CfRatingChange;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CodeforcesApiClientTest {

    private CodeforcesApiClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RateLimiter rateLimiter = RateLimiter.create(100.0); // Fast for tests
        RestTemplate restTemplate = new RestTemplate();
        client = new CodeforcesApiClient(restTemplate, rateLimiter);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void userStatus_HandleNotFound_ThrowsException() {
        String url = "https://codeforces.com/api/user.status?handle=unknown&from=1&count=500";
        mockServer.expect(requestTo(url))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"FAILED\",\"comment\":\"handles: User with handle unknown not found\"}"));

        assertThatThrownBy(() -> client.userStatus("unknown", 1, 500))
                .isInstanceOf(CodeforcesApiClient.HandleNotFoundException.class)
                .hasMessageContaining("not found");

        mockServer.verify();
    }

    @Test
    void userStatus_RetryOn5xx_Success() {
        String url = "https://codeforces.com/api/user.status?handle=test&from=1&count=500";
        mockServer.expect(requestTo(url))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        mockServer.expect(requestTo(url))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"OK\",\"result\":[]}"));

        List<?> result = client.userStatus("test", 1, 500);
        assertThat(result).isEmpty();

        mockServer.verify();
    }

    /**
     * The regression behind half the club failing CF contest sync: a non-empty
     * user.rating result could not be read at all under Spring Boot 4's Jackson 3,
     * because the client asked for Jackson 2 JsonNodes.
     */
    @Test
    void userRating_nonEmptyHistory_isReadIntoTypedRows() {
        mockServer.expect(requestTo("https://codeforces.com/api/user.rating?handle=tourist"))
                .andRespond(withSuccess("""
                        {"status":"OK","result":[
                          {"contestId":1,"contestName":"Round 1","handle":"tourist","rank":3,
                           "ratingUpdateTimeSeconds":1700000000,"oldRating":0,"newRating":1500,"extra":"ignored"}
                        ]}""", MediaType.APPLICATION_JSON));

        List<CfRatingChange> changes = client.userRating("tourist");

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getContestId()).isEqualTo(1);
        assertThat(changes.get(0).getNewRating()).isEqualTo(1500);
        assertThat(changes.get(0).getRank()).isEqualTo(3);
        mockServer.verify();
    }

    @Test
    void contestList_readsUnscheduledRoundsWithoutStartTime() {
        mockServer.expect(requestTo("https://codeforces.com/api/contest.list?gym=false"))
                .andRespond(withSuccess("""
                        {"status":"OK","result":[
                          {"id":2000,"name":"Div. 2","phase":"BEFORE","durationSeconds":7200,"startTimeSeconds":1800000000},
                          {"id":2001,"name":"TBA","phase":"BEFORE","durationSeconds":7200}
                        ]}""", MediaType.APPLICATION_JSON));

        List<CfContest> contests = client.contestList(false);

        assertThat(contests).extracting(CfContest::getId).containsExactly(2000, 2001);
        assertThat(contests.get(1).getStartTimeSeconds()).isNull();
    }

    @Test
    void handleWithUrlCharacters_isEncodedNotInterpolated() {
        mockServer.expect(requestTo("https://codeforces.com/api/user.rating?handle=a%26b"))
                .andRespond(withSuccess("{\"status\":\"OK\",\"result\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.userRating("a&b")).isEmpty();
        mockServer.verify();
    }

    @Test
    void callLimitExceeded_isRetried() {
        String url = "https://codeforces.com/api/user.rating?handle=x";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(
                "{\"status\":\"FAILED\",\"comment\":\"Call limit exceeded\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(url)).andRespond(withSuccess(
                "{\"status\":\"OK\",\"result\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.userRating("x")).isEmpty();
        mockServer.verify();
    }
}
