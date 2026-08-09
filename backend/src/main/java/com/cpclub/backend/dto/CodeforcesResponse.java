package com.cpclub.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class CodeforcesResponse {
    private String status;
    private List<CodeforcesUserDto> result;
    private String comment;
}
