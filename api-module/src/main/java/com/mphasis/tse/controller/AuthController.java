package com.mphasis.tse.controller;

import com.mphasis.tse.dto.ApiResponse;
import com.mphasis.tse.dto.LoginRequest;
import com.mphasis.tse.filter.AuthenticationService;
import com.mphasis.tse.filter.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;
    private final UserService userService;
    @SecurityRequirements
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody LoginRequest req) {

        authService.register(req.getEmail(), req.getPassword());
        return ResponseEntity.ok().body(new ApiResponse<>(
                HttpStatus.OK.name(),
                HttpStatus.OK.value(),
                "User Created Successfully",
                null
        ));

    }

    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest req) {
        String token = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok().body(new ApiResponse<>(
                HttpStatus.OK.name(),
                HttpStatus.OK.value(),
                "Login Successfull",
                token
        ));

    }
}