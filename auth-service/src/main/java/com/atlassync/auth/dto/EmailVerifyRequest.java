package com.atlassync.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerifyRequest(
        @NotBlank
        @Pattern(regexp = "\\d{4,10}", message = "Code must be 4-10 digits")
        String code
) {}
