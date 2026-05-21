package com.mphasis.tse.filter;

import com.mphasis.tse.impl.AuthenticationServiceImpl;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)

class AuthenticationServiceTest {

    @InjectMocks

    private AuthenticationServiceImpl authenticationService;

    @Test

    void service_shouldLoad() {

        assertNotNull(authenticationService);

    }

}
