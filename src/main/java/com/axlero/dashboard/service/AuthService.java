package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.AuthResponse;
import com.axlero.dashboard.dto.LoginRequest;
import com.axlero.dashboard.dto.RegisterRequest;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                jwtService.generateToken(savedUser.getUsername()),
                "Bearer",
                savedUser.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid username/email or password"));

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new BadCredentialsException(
                    "Invalid username or password");
        }

        return new AuthResponse(
                jwtService.generateToken(user.getUsername()),
                "Bearer",
                user.getUsername());
    }
}