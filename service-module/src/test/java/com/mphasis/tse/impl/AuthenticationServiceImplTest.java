package com.mphasis.tse.impl;

import com.mphasis.tse.entity.User;
import com.mphasis.tse.exception.DuplicateUserException;
import com.mphasis.tse.filter.JwtService;
import com.mphasis.tse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository repo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationServiceImpl service;


    @ParameterizedTest
    @CsvSource({
            "user1@mail.com, pass1",
            "user2@mail.com, pass2",
            "test@mail.com, 1234"
    })
    void register_success_multipleInputs(String email, String password) {

        when(repo.existsByEmail(email)).thenReturn(false);
        when(encoder.encode(password)).thenReturn("encoded");

        service.register(email, password);

        verify(repo).save(any(User.class));
        verify(encoder).encode(password);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "dup@mail.com",
            "test@mail.com"
    })
    void register_shouldThrowException_forDuplicateEmails(String email) {

        when(repo.existsByEmail(email)).thenReturn(true);

        assertThrows(DuplicateUserException.class, () ->
                service.register(email, "password")
        );

        verify(repo, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "user1@mail.com, pass1",
            "user2@mail.com, pass2"
    })
    void login_success_multipleUsers(String email, String password) {

        User user = new User();
        user.setEmail(email);

        when(repo.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token");

        String result = service.login(email, password);

        assertEquals("token", result);
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

     @ParameterizedTest
    @ValueSource(strings = {
            "wrong1",
            "wrong2"
    })
    void login_shouldFail_forInvalidPasswords(String password) {

        doThrow(new RuntimeException("Bad credentials"))
                .when(authManager)
                .authenticate(any());

        assertThrows(RuntimeException.class, () ->
                service.login("test@mail.com", password)
        );
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {

        when(repo.findByEmail("test@mail.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                service.login("test@mail.com", "1234")
        );
    }
}
