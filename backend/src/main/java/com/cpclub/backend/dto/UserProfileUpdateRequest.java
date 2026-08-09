package com.cpclub.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,24}$", message = "Invalid Codeforces handle format")
    private String codeforcesHandle;
}
