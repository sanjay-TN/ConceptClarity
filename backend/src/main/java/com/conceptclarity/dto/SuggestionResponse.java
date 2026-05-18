package com.conceptclarity.dto;

import java.util.List;

public record SuggestionResponse(
        List<String> suggestions
) {
}
