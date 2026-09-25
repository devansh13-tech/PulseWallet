package com.pulsewallet.pulsewallet.dto;

import com.pulsewallet.pulsewallet.entity.User;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {

    public static AuthResponse of(String token, long expiresInSeconds, User user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, UserResponse.from(user));
    }
}
