package com.cpclub.backend.leaderboard.service;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardEntryProjection;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     *
     * <p>Ranking is done by the database in the same query that fetches the page.
     * The previous implementation issued one {@code COUNT} per row, so rendering a
     * default page of 20 members cost 22 round trips and grew linearly with page
     * size — up to 102 at the maximum permitted size of 100.</p>
     *
     * <p>Ranking semantics are unchanged: tied ratings share a position, the next
     * distinct rating skips the gap, and unrated members tie on a single rank at
     * the end.</p>
     *
     * @param page zero-indexed page number
     * @param size items per page limit
     * @return paginated response containing ranked users
     */
    @Transactional(readOnly = true)
    public PagedResponse<LeaderboardResponseDto> getLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaderboardEntryProjection> rankedPage = userRepository.findLeaderboardPage(pageable);

        List<LeaderboardResponseDto> content = rankedPage.getContent().stream()
                .map(LeaderboardResponseDto::fromProjection)
                .toList();

        return new PagedResponse<>(
                content,
                rankedPage.getNumber(),
                rankedPage.getSize(),
                rankedPage.getTotalElements(),
                rankedPage.getTotalPages(),
                rankedPage.isLast()
        );
    }
}

