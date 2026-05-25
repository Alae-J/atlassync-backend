package com.atlassync.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{8,15}$")
        String phone,

        @NotBlank
        @Pattern(regexp = "\\d{4,10}", message = "Code must be 4-10 digits")
        String code
) {}
