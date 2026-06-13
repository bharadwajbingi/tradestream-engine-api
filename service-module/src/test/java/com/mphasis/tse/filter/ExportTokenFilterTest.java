package com.mphasis.tse.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportTokenFilterTest {

    @Mock
    private ExportTokenService exportTokenService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private ExportTokenFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new ExportTokenFilter(exportTokenService, userDetailsService);
    }

    @Test
    void exportPath_withTokenForDifferentAuthenticatedUser_shouldReject() throws Exception {
        MockHttpServletRequest request = exportRequest();
        request.addHeader("X-Export-Token", "export-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        com.mphasis.tse.entity.User user = user("current@example.com", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        when(exportTokenService.validateExportTokenAndGetEmail("export-token"))
                .thenReturn("other@example.com");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void exportPath_withTokenForAuthenticatedUser_shouldContinue() throws Exception {
        MockHttpServletRequest request = exportRequest();
        request.addHeader("X-Export-Token", "export-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        com.mphasis.tse.entity.User user = user("current@example.com", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        when(exportTokenService.validateExportTokenAndGetEmail("export-token"))
                .thenReturn("current@example.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void exportPath_withoutTokenForUserWithoutTotp_shouldContinue() throws Exception {
        MockHttpServletRequest request = exportRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        com.mphasis.tse.entity.User user = user("current@example.com", false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(exportTokenService, never()).validateExportTokenAndGetEmail(org.mockito.ArgumentMatchers.anyString());
    }

    private MockHttpServletRequest exportRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/transactions/export");
        return request;
    }

    private com.mphasis.tse.entity.User user(String email, boolean totpEnabled) {
        com.mphasis.tse.entity.User user = new com.mphasis.tse.entity.User();
        user.setEmail(email);
        user.setTotpEnabled(totpEnabled);
        return user;
    }
}
