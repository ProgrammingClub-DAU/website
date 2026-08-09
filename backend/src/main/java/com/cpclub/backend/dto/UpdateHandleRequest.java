package com.cpclub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHandleRequest {

    @NotBlank(message = "Codeforces handle cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,24}$", message = "Invalid Codeforces handle format")
    private String codeforcesHandle;
}
