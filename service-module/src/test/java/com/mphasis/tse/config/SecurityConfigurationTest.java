package com.mphasis.tse.config;

import com.mphasis.tse.filter.JwtAuthenticationFilter;
import com.mphasis.tse.filter.ExportTokenFilter;
import com.mphasis.tse.config.oauth2.OAuth2SuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityConfigurationTest {
    private SecurityConfiguration securityConfiguration;
    private JwtAuthenticationFilter jwtFilter;
    private AuthenticationProvider authProvider;
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    private ExportTokenFilter exportTokenFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = mock(JwtAuthenticationFilter.class);
        authProvider = mock(AuthenticationProvider.class);
        oAuth2SuccessHandler = mock(OAuth2SuccessHandler.class);
        exportTokenFilter = mock(ExportTokenFilter.class);
        securityConfiguration = new SecurityConfiguration(jwtFilter, authProvider, oAuth2SuccessHandler, exportTokenFilter);
    }

    private HttpSecurity mockHttp() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authenticationProvider(any())).thenReturn(http);
        when(http.oauth2Login(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any())).thenReturn(http);
        when(http.addFilterAfter(any(), any())).thenReturn(http);
        SecurityFilterChain chain = mock(SecurityFilterChain.class);
        doReturn(chain).when(http).build();
        return http;
    }

    @Test
    void testSecurityFilterChain() throws Exception {
        HttpSecurity http = mockHttp();
        SecurityFilterChain result = securityConfiguration.securityFilterChain(http);
        assertNotNull(result);
        verify(http).authenticationProvider(authProvider);
        verify(http, atLeastOnce()).addFilterBefore(any(), any());
        verify(http).build();
    }

    @Test
    void testLambdaConfigurations() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        ArgumentCaptor<Customizer> authCaptor = ArgumentCaptor.forClass(Customizer.class);
        ArgumentCaptor<Customizer> sessionCaptor = ArgumentCaptor.forClass(Customizer.class);
        
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.authorizeHttpRequests(authCaptor.capture())).thenReturn(http);
        when(http.sessionManagement(sessionCaptor.capture())).thenReturn(http);
        when(http.authenticationProvider(any())).thenReturn(http);
        when(http.oauth2Login(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any())).thenReturn(http);
        when(http.addFilterAfter(any(), any())).thenReturn(http);
        SecurityFilterChain chain = mock(SecurityFilterChain.class);
        doReturn(chain).when(http).build();
        
        securityConfiguration.securityFilterChain(http);

        var registry = mock(
                org.springframework.security.config.annotation.web.configurers
                        .AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class
        );
        var authorizedUrl = mock(
                org.springframework.security.config.annotation.web.configurers
                        .AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class
        );
        when(registry.requestMatchers(anyString())).thenReturn(authorizedUrl);
        when(registry.requestMatchers(any(String[].class))).thenReturn(authorizedUrl);
        when(authorizedUrl.permitAll()).thenReturn(registry);
        when(registry.anyRequest()).thenReturn(authorizedUrl);
        when(authorizedUrl.authenticated()).thenReturn(registry);
        
        authCaptor.getValue().customize(registry);
        
        verify(registry, atLeastOnce()).requestMatchers(anyString());
        verify(authorizedUrl, atLeastOnce()).permitAll();
        verify(registry).anyRequest();
        verify(authorizedUrl, atLeastOnce()).authenticated();

        var sessionConfig = mock(
                org.springframework.security.config.annotation.web.configurers
                        .SessionManagementConfigurer.class
        );
        when(sessionConfig.sessionCreationPolicy(any())).thenReturn(sessionConfig);
        sessionCaptor.getValue().customize(sessionConfig);
        verify(sessionConfig).sessionCreationPolicy(
                org.springframework.security.config.http.SessionCreationPolicy.STATELESS
        );
    }

    @Test
    void testCorsConfiguration() {
        CorsConfigurationSource source = securityConfiguration.corsConfigurationSource();
        assertNotNull(source);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        var config = source.getCorsConfiguration(request);
        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:4200"));
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedHeaders().contains("*"));
        assertTrue(config.getAllowCredentials());
    }
}
