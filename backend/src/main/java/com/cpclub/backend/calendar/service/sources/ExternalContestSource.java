package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.dto.ExternalContestDto;

import java.util.List;

public interface ExternalContestSource {
    ContestPlatform platform();
    List<ExternalContestDto> fetchUpcoming();
}
