package com.conceptclarity.service;

import com.conceptclarity.dto.AuthResponse;
import com.conceptclarity.dto.LoginRequest;
import com.conceptclarity.dto.RegisterRequest;
import com.conceptclarity.exception.BadRequestException;
import com.conceptclarity.model.User;
import com.conceptclarity.repository.UserRepository;
import java.time.LocalDateTime;
import com.conceptclarity.security.PasswordService;
import com.conceptclarity.util.InputSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final InputSanitizer inputSanitizer;

    public UserService(UserRepository userRepository,
                       PasswordService passwordService,
                       InputSanitizer inputSanitizer) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.inputSanitizer = inputSanitizer;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = inputSanitizer.cleanEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists.");
        }

        String salt = passwordService.generateSalt();
        User user = User.builder()
                .name(inputSanitizer.cleanText(request.name(), 80))
                .email(email)
                .passwordSalt(salt)
                .passwordHash(passwordService.hashPassword(request.password(), salt))
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        return new AuthResponse(saved.getId(), saved.getName(), saved.getEmail(), "Registration successful.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = inputSanitizer.cleanEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        boolean valid = passwordService.matches(request.password(), user.getPasswordSalt(), user.getPasswordHash());
        if (!valid && passwordService.matchesLegacySha256(request.password(), user.getPasswordSalt(), user.getPasswordHash())) {
            user.setPasswordHash(passwordService.hashPassword(request.password(), user.getPasswordSalt()));
            valid = true;
        }
        if (!valid) {
            throw new BadRequestException("Invalid email or password.");
        }

        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), "Login successful.");
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User session is invalid. Please log in again."));
    }

}
