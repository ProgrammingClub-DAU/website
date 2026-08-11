package com.cpclub.backend.codeforces;

import com.cpclub.backend.codeforces.dto.CodeforcesResponse;
import com.cpclub.backend.codeforces.dto.CodeforcesUserDto;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).codeforcesHandle("tourist").rating(3000).build();
    }

    @Test
    void syncCodeforcesRatings_Success() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(user1));

        CodeforcesUserDto dto = new CodeforcesUserDto("tourist", 4000, 4000, "legendary grandmaster", "legendary grandmaster");
        CodeforcesResponse response = new CodeforcesResponse("OK", null, List.of(dto));

        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class))).thenReturn(response);

        codeforcesSyncService.syncCodeforcesRatings();

        verify(userRepository, times(1)).save(user1);
        assert(user1.getRating() == 4000);
    }
}
