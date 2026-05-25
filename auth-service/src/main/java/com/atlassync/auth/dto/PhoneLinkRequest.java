package com.atlassync.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneLinkRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Use 8-15 digits, optional leading +")
        String phone
) {}
