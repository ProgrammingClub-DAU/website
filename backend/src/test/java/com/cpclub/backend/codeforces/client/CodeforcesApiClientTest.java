package com.cpclub.backend.codeforces.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CodeforcesApiClientTest {

    private CodeforcesApiClient client;
    private MockRestServiceServer mockServer;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        RateLimiter rateLimiter = RateLimiter.create(100.0); // Fast for tests
        restTemplate = new RestTemplate();
        client = new CodeforcesApiClient(rateLimiter);
        
        // Inject our restTemplate into the client using reflection since we didn't add a setter
        try {
            java.lang.reflect.Field field = CodeforcesApiClient.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(client, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
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
}
