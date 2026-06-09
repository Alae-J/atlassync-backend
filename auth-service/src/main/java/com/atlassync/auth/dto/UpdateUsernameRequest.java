package com.atlassync.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(
        @NotBlank
        @Size(min = 2, max = 60)
        @Pattern(regexp = "^[^\\s].*[^\\s]$|^[^\\s]$",
                message = "Cannot start or end with whitespace")
        String username
) {}
