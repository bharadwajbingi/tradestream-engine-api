package com.mphasis.tse.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mphasis.tse.dto.ApiResponse;
import com.mphasis.tse.dto.LoginRequest;
import com.mphasis.tse.filter.AuthenticationService;
import com.mphasis.tse.filter.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest request;

    @BeforeEach
    void setUp() {
        request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
    }

    @Test
    void signup_shouldReturnSuccessResponse() {

        ResponseEntity<ApiResponse<String>> response =
                authController.signup(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User Created Successfully", response.getBody().getMessage());
        assertEquals(200, response.getBody().getCode());
        assertEquals("OK", response.getBody().getStatus());

        verify(authService).register("test@example.com", "password123");
    }

    @Test
    void login_shouldReturnToken() {

        when(authService.login("test@example.com", "password123"))
                .thenReturn("jwt-token");

        ResponseEntity<ApiResponse<String>> response =
                authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Login Successfull", response.getBody().getMessage());
        assertEquals("jwt-token", response.getBody().getData());

        verify(authService).login("test@example.com", "password123");
    }
}