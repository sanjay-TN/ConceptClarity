package com.conceptclarity.controller;

import com.conceptclarity.dto.ExplanationRequest;
import com.conceptclarity.dto.ExplanationResponse;
import com.conceptclarity.service.ExplanationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExplanationController {

    private final ExplanationService explanationService;

    public ExplanationController(ExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    @PostMapping({"/explain", "/api/explanations"})
    public ExplanationResponse explain(@Valid @RequestBody ExplanationRequest request) {
        return explanationService.explain(request);
    }
}
