package com.cpclub.backend.leaderboard.service;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class performing leaderboard computations, rank mappings,
 * and retrieval of paginated club rankings.
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;

    /**
     * Resolves ranked users sorted by current rating.
     * Computes absolute rank based on page offsets.
     *
     * @param page zero-indexed page number
     * @param size items per page limit
     * @return paginated response containing ranked users
     */
    @Transactional(readOnly = true)
    public PagedResponse<LeaderboardResponseDto> getLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAllByOrderByRatingDescNullsLast(pageable);

        // Absolute rank starts from page boundary index + 1
        int startRank = page * size + 1;
        List<LeaderboardResponseDto> content = new ArrayList<>();
        for (int i = 0; i < userPage.getContent().size(); i++) {
            User user = userPage.getContent().get(i);
            content.add(LeaderboardResponseDto.fromEntity(user, startRank + i));
        }

        return new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }
}

