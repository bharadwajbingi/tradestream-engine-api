package com.mphasis.tse.filter;

import com.mphasis.tse.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;
    @Test
    void service_shouldNotBeNull() {
        assertNotNull(userService);
    }

    @Test
    void dummyTest() {
        assertTrue(true);
    }
}