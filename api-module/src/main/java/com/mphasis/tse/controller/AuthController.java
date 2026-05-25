package com.mphasis.tse.controller;

import com.mphasis.tse.dto.ApiResponse;
import com.mphasis.tse.dto.LoginRequest;
import com.mphasis.tse.dto.LoginResponse;
import com.mphasis.tse.dto.RegisterRequest;
import com.mphasis.tse.filter.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authenticationService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "Registration successful.", null
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        String token = authenticationService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(
                "OK", 200, "Login successful.", new LoginResponse(token)
        ));
    }
}