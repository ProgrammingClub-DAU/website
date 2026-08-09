package com.cpclub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHandleRequest {

    @NotBlank(message = "Codeforces handle cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,24}$", message = "Invalid Codeforces handle format")
    private String codeforcesHandle;
}
