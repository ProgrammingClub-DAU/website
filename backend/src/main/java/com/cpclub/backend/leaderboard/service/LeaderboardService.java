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

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<LeaderboardResponseDto> getLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAllByOrderByRatingDescNullsLast(pageable);

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
