package com.conceptclarity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        Long userId,
        @NotBlank @Size(min = 2, max = 1000) String message
) {
}
