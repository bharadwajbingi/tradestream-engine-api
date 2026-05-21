package com.mphasis.tse.impl;

import com.mphasis.tse.dto.ProfileResponse;
import com.mphasis.tse.entity.User;
import com.mphasis.tse.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repo;

    @InjectMocks
    private UserServiceImpl service;


    @ParameterizedTest
    @ValueSource(strings = {
            "user1@mail.com",
            "test@mail.com",
            "abc@mail.com"
    })
    void getProfile_success(String email) {

        User user = new User();
        user.setEmail(email);

        when(repo.findByEmail(email)).thenReturn(Optional.of(user));

        ProfileResponse response = service.getProfile(email);

        assertEquals(email, response.getEmail());
        verify(repo).findByEmail(email);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "notfound@mail.com",
            "missing@mail.com"
    })
    void getProfile_shouldThrow_whenUserNotFound(String email) {

        when(repo.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.getProfile(email)
        );

        assertEquals("User not found", ex.getMessage());
        verify(repo).findByEmail(email);
    }
}