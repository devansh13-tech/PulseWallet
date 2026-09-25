package com.pulsewallet.pulsewallet.dto;

import java.time.Instant;

import com.pulsewallet.pulsewallet.entity.User;

/** Never includes {@code passwordHash} - that field must never leave the service layer. */
public record UserResponse(Long id, String name, String email, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
