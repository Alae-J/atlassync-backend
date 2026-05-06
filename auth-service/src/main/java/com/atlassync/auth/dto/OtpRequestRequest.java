package com.atlassync.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequestRequest(
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$",
                message = "phone must be E.164 (e.g. +14155552671)")
        String phone
) {}
