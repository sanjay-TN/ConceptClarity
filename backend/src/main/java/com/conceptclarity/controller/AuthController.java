package com.conceptclarity.controller;

import com.conceptclarity.dto.AuthResponse;
import com.conceptclarity.dto.LoginRequest;
import com.conceptclarity.dto.RegisterRequest;
import com.conceptclarity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping({"/register", "/api/auth/register"})
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping({"/login", "/api/auth/login"})
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
