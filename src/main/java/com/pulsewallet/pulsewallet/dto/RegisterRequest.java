package com.pulsewallet.pulsewallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param password plaintext, 8-72 chars. The 72 ceiling is BCrypt's own input
 *                  limit (it silently ignores bytes past 72), not an
 *                  arbitrary choice.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
