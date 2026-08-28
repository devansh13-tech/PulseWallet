package com.pulsewallet.pulsewallet.dto;

import java.time.Instant;

import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.TransactionType;

/** @param system true for a seeded default category; false for a user-created one. */
public record CategoryResponse(Long id, String name, TransactionType type, boolean system, Instant createdAt) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getUser() == null,
                category.getCreatedAt());
    }
}
