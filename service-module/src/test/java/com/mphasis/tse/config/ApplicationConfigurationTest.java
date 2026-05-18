package com.mphasis.tse.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.mphasis.tse.repository.UserRepository;
import com.mphasis.tse.entity.User;

class ApplicationConfigurationTest {
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationConfiguration authenticationConfiguration;
    @Mock
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        applicationConfiguration = new ApplicationConfiguration(userRepository);
    }

    @Test
    void testUserDetailsService_UserFound() {
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        UserDetailsService service = applicationConfiguration.userDetailsService();
        UserDetails result = service.loadUserByUsername("test@example.com");
        assertNotNull(result);
    }

    @Test
    void testUserDetailsService_UserNotFound() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        UserDetailsService service = applicationConfiguration.userDetailsService();
        assertThrows(UsernameNotFoundException.class, () -> {
            service.loadUserByUsername("test@example.com");
        });
    }

    @Test
    void testPasswordEncoder() {
        BCryptPasswordEncoder encoder = applicationConfiguration.passwordEncoder();
        assertNotNull(encoder);
        String encoded = encoder.encode("password");
        assertTrue(encoder.matches("password", encoded));
    }

    @Test
    void testAuthenticationManager(){
        when(authenticationConfiguration.getAuthenticationManager())
                .thenReturn(authenticationManager);
        AuthenticationManager result =
                applicationConfiguration.authenticationManager(authenticationConfiguration);
        assertNotNull(result);
        assertEquals(authenticationManager, result);
    }

    @Test
    void testAuthenticationProvider() {
        DaoAuthenticationProvider provider =
                (DaoAuthenticationProvider) applicationConfiguration.authenticationProvider();
        assertNotNull(provider);
        assertTrue(provider instanceof DaoAuthenticationProvider);
    }
}