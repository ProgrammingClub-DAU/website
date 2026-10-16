package com.cpclub.backend.contest.service;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.contest.dto.SeasonDto;
import com.cpclub.backend.contest.dto.SeasonRequest;
import com.cpclub.backend.contest.entity.ClubSeason;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubSeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private final ClubSeasonRepository seasonRepository;
    private final ClubContestRepository contestRepository;

    @Transactional(readOnly = true)
    public List<SeasonDto> list() {
        return seasonRepository.findAllByOrderByStartsOnDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public SeasonDto create(SeasonRequest request) {
        validate(request);
        ClubSeason season = seasonRepository.save(ClubSeason.builder()
                .name(request.getName().trim())
                .startsOn(request.getStartsOn())
                .endsOn(request.getEndsOn())
                .countBest(request.getCountBest())
                .build());
        return toDto(season);
    }

    @Transactional
    public SeasonDto update(Long id, SeasonRequest request) {
        validate(request);
        ClubSeason season = find(id);
        season.setName(request.getName().trim());
        season.setStartsOn(request.getStartsOn());
        season.setEndsOn(request.getEndsOn());
        season.setCountBest(request.getCountBest());
        return toDto(seasonRepository.save(season));
    }

    /** The season goes; its contests stay, outside any season. */
    @Transactional
    public void delete(Long id) {
        ClubSeason season = find(id);
        contestRepository.detachFromSeason(id);
        seasonRepository.delete(season);
    }

    private ClubSeason find(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id: " + id));
    }

    private static void validate(SeasonRequest request) {
        // Mirrors club_seasons_dates_check, so the admin gets a message rather than a 500.
        if (request.getEndsOn().isBefore(request.getStartsOn())) {
            throw new BadRequestException("A season cannot end before it starts");
        }
    }

    private SeasonDto toDto(ClubSeason season) {
        return SeasonDto.builder()
                .id(season.getId())
                .name(season.getName())
                .startsOn(season.getStartsOn())
                .endsOn(season.getEndsOn())
                .countBest(season.getCountBest())
                .contestCount(contestRepository.countBySeasonId(season.getId()))
                .build();
    }
}
