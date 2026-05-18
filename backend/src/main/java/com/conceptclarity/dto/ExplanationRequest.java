package com.conceptclarity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExplanationRequest(
        @NotNull Long userId,
        @NotBlank @Size(min = 2, max = 240) String topic,
        @NotBlank String level,
        @NotBlank String explanationType
) {
}
