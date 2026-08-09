package com.cpclub.backend.service;

import com.cpclub.backend.dto.CodeforcesResponse;
import com.cpclub.backend.dto.CodeforcesUserDto;
import com.cpclub.backend.entity.User;
import com.cpclub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CodeforcesSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CodeforcesSyncService codeforcesSyncService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).codeforcesHandle("tourist").rating(3000).build();
        user2 = User.builder().id(2L).codeforcesHandle("invalid_handle").rating(1000).build();
    }

    @Test
    void syncCodeforcesRatings_Success() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(user1));

        CodeforcesUserDto dto = new CodeforcesUserDto();
        dto.setHandle("tourist");
        dto.setRating(4000); // Updated rating
        
        CodeforcesResponse response = new CodeforcesResponse();
        response.setStatus("OK");
        response.setResult(List.of(dto));

        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class))).thenReturn(response);

        codeforcesSyncService.syncCodeforcesRatings();

        verify(userRepository, times(1)).save(user1);
        assert(user1.getRating() == 4000);
    }

    @Test
    void syncCodeforcesRatings_ApiFailureContinuesLoop() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(user2, user1));

        // First call fails
        when(restTemplate.getForObject(contains("invalid_handle"), eq(CodeforcesResponse.class)))
                .thenThrow(new RestClientException("400 Bad Request"));

        // Second call succeeds
        CodeforcesUserDto dto = new CodeforcesUserDto();
        dto.setHandle("tourist");
        dto.setRating(3500);
        
        CodeforcesResponse response = new CodeforcesResponse();
        response.setStatus("OK");
        response.setResult(List.of(dto));

        when(restTemplate.getForObject(contains("tourist"), eq(CodeforcesResponse.class)))
                .thenReturn(response);

        codeforcesSyncService.syncCodeforcesRatings();

        // Only user1 should be saved with updated rating
        verify(userRepository, never()).save(user2);
        verify(userRepository, times(1)).save(user1);
        assert(user1.getRating() == 3500);
        assert(user2.getRating() == 1000); // Unchanged
    }
}
