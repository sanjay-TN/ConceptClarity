package com.conceptclarity.controller;

import com.conceptclarity.dto.ApiMessageResponse;
import com.conceptclarity.dto.ExplanationResponse;
import com.conceptclarity.dto.PageResponse;
import com.conceptclarity.dto.ProgressResponse;
import com.conceptclarity.dto.SuggestionResponse;
import com.conceptclarity.service.HistoryService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history")
    public List<ExplanationResponse> history(@RequestParam Long userId,
                                             @RequestParam(required = false) String search) {
        return historyService.history(userId, search);
    }

    @GetMapping("/api/history")
    public PageResponse<ExplanationResponse> historyPage(@RequestParam Long userId,
                                                         @RequestParam(required = false) String search,
                                                         @RequestParam(defaultValue = "false") boolean favoriteOnly,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "8") int size) {
        return historyService.historyPage(userId, search, favoriteOnly, page, size);
    }

    @DeleteMapping({"/history/{id}", "/api/history/{id}"})
    public ApiMessageResponse deleteHistory(@PathVariable Long id, @RequestParam Long userId) {
        return historyService.deleteHistory(userId, id);
    }

    @PostMapping({"/favorite/{id}", "/api/favorites/{id}"})
    public ExplanationResponse toggleFavorite(@PathVariable Long id, @RequestParam Long userId) {
        return historyService.toggleFavorite(userId, id);
    }

    @GetMapping("/api/suggestions")
    public SuggestionResponse suggestions(@RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) String topic) {
        return historyService.suggestions(userId, topic);
    }

    @GetMapping("/api/progress")
    public ProgressResponse progress(@RequestParam Long userId) {
        return historyService.progress(userId);
    }
}
