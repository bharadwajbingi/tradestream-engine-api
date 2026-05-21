package com.mphasis.tse.filter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @InjectMocks
    private JwtAuthenticationFilter filter;
    @Mock
    private JwtService jwtService;
    @Mock
    private FilterChain filterChain;
    @Mock
    private UserDetailsService userDetailsService;
    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authPath_shouldSkipFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void noHeader_shouldPass() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void invalidHeaderFormat_shouldPass() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void invalidToken_shouldPass() throws Exception {
        String token = "invalid.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername(token))
                .thenThrow(new RuntimeException("Invalid JWT"));
        filter.doFilterInternal(request, response, filterChain);
        verify(jwtService).extractUsername(token);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void nullUsername_shouldSkipAuthentication() throws Exception {
        String token = "token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername(token)).thenReturn(null);
        filter.doFilterInternal(request, response, filterChain);
        verify(jwtService).extractUsername(token);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void alreadyAuthenticated_shouldSkipSettingAuth() throws Exception {
        String token = "token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing", null, null)
        );
        when(jwtService.extractUsername(token)).thenReturn("admin");
        filter.doFilterInternal(request, response, filterChain);
        verify(jwtService).extractUsername(token);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidTokenValidation_shouldNotSetAuth() throws Exception {
        String token = "token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername(token)).thenReturn("admin");
        UserDetails user = User.withUsername("admin")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        when(jwtService.isTokenValid(token, user)).thenReturn(false);
        filter.doFilterInternal(request, response, filterChain);
        verify(userDetailsService).loadUserByUsername("admin");
        verify(jwtService).isTokenValid(token, user);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_shouldSetAuthentication() throws Exception {
        String token = "valid.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername(token)).thenReturn("admin");
        UserDetails user = User.withUsername("admin")
                .password("password")
                .authorities("ROLE_ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        when(jwtService.isTokenValid(token, user)).thenReturn(true);
        filter.doFilterInternal(request, response, filterChain);
        verify(jwtService).extractUsername(token);
        verify(userDetailsService).loadUserByUsername("admin");
        verify(jwtService).isTokenValid(token, user);
        verify(filterChain).doFilter(request, response);
    }
}
