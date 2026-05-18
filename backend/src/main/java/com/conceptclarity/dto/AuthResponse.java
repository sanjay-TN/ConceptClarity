package com.conceptclarity.dto;

public record AuthResponse(
        Long userId,
        String name,
        String email,
        String message
) {
}
