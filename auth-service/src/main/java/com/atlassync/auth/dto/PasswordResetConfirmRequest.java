package com.atlassync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank @Email(message = "Provide a valid email") String email,

        @NotBlank
        @Pattern(regexp = "\\d{4,10}", message = "Code must be 4-10 digits")
        String code,

        @NotBlank @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        String newPassword
) {}
