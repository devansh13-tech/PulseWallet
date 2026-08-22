package com.pulsewallet.pulsewallet.dto;

import com.pulsewallet.pulsewallet.entity.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull TransactionType type) {
}
