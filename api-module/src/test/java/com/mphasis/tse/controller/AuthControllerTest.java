package com.mphasis.tse.controller;

import com.mphasis.tse.dto.ApiResponse;
import com.mphasis.tse.dto.LoginRequest;
import com.mphasis.tse.dto.LoginResponse;
import com.mphasis.tse.dto.RegisterRequest;
import com.mphasis.tse.exception.DuplicateUserException;
import com.mphasis.tse.filter.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 * Tests controller methods directly with a mocked AuthenticationService.
 *
 * Note: @Valid annotation-based validation (MethodArgumentNotValidException) does not trigger
 * in unit tests — that requires a WebMvcTest or integration test with the full Spring context.
 *
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@example.com");
        registerRequest.setPassword("securePass123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("securePass123");
    }

    @Test
    @DisplayName("Successful registration returns 200 with 'Registration successful.' message")
    void register_withValidRequest_returnsSuccessResponse() {
        // Arrange: register() does nothing (void method, no exception)
        doNothing().when(authenticationService).register("user@example.com", "securePass123");

        // Act
        ResponseEntity<ApiResponse<Void>> response = authController.register(registerRequest);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("OK");
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Registration successful.");
        assertThat(response.getBody().getData()).isNull();

        verify(authenticationService).register("user@example.com", "securePass123");
    }

    @Test
    @DisplayName("Successful login returns 200 with JWT token in LoginResponse")
    void login_withValidCredentials_returnsTokenResponse() {
        // Arrange
        when(authenticationService.login("user@example.com", "securePass123"))
                .thenReturn("jwt-token-123");

        // Act
        ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(loginRequest);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("OK");
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Login successful.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token-123");

        verify(authenticationService).login("user@example.com", "securePass123");
    }

    @Test
    @DisplayName("Duplicate registration throws DuplicateUserException")
    void register_withDuplicateEmail_throwsDuplicateUserException() {
        // Arrange
        doThrow(new DuplicateUserException("Email is already registered"))
                .when(authenticationService).register("user@example.com", "securePass123");

        // Act & Assert
        assertThatThrownBy(() -> authController.register(registerRequest))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessage("Email is already registered");

        verify(authenticationService).register("user@example.com", "securePass123");
    }

    @Test
    @DisplayName("Invalid credentials throws BadCredentialsException")
    void login_withInvalidCredentials_throwsBadCredentialsException() {
        // Arrange
        when(authenticationService.login("user@example.com", "securePass123"))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        // Act & Assert
        assertThatThrownBy(() -> authController.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(authenticationService).login("user@example.com", "securePass123");
    }

    // Note: Validation failure testing (e.g., blank email triggering @NotBlank) requires
    // a WebMvcTest or integration test with the full Spring MVC context, as @Valid
    // annotation processing is handled by the framework's argument resolver, not the
    // controller method itself. In a unit test, the controller receives the DTO directly
    // without validation being triggered.
}
