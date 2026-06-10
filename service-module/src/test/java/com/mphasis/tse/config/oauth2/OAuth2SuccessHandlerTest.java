package com.mphasis.tse.config.oauth2;

import com.mphasis.tse.entity.User;
import com.mphasis.tse.enums.AuthProvider;
import com.mphasis.tse.filter.JwtService;
import com.mphasis.tse.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for OAuth2SuccessHandler — the handler that fires after Google login succeeds.
 *
 * Key behaviours verified:
 *  - Existing user: finds from DB, generates JWT, redirects to frontend
 *  - New user: creates user with GOOGLE provider, saves, generates JWT
 *  - Missing email from OAuth2 provider: returns 400 Bad Request
 *  - Redirect URL contains the JWT token as a query param
 *  - User name is updated if previously null
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2SuccessHandler — Google OAuth2 login flow")
class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2SuccessHandler handler;

    private static final String EMAIL      = "bharadwaj@example.com";
    private static final String NAME       = "Bharadwaj Bingi";
    private static final String JWT_TOKEN  = "eyJhbGciOiJIUzI1NiJ9.test.token";
    private static final String REDIRECT   = "http://localhost:5173/oauth2/redirect";

    @BeforeEach
    void setUp() {
        // Inject the frontend redirect URL (normally comes from @Value)
        ReflectionTestUtils.setField(handler, "frontendRedirectUrl", REDIRECT);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
    }

    // -----------------------------------------------------------------------
    // Existing user flow
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("existing user — finds from DB, generates JWT, redirects with token")
    void onSuccess_existingUser_redirectsWithJwt() throws Exception {
        User existingUser = existingUser(EMAIL, NAME);
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        when(oAuth2User.getAttribute("name")).thenReturn(NAME);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn(JWT_TOKEN);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Verify redirect happened with the JWT token
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        String redirectUrl = redirectCaptor.getValue();
        assertTrue(redirectUrl.startsWith(REDIRECT), "Must redirect to configured frontend URL");
        assertTrue(redirectUrl.contains("token=" + JWT_TOKEN), "Redirect URL must contain JWT token");
    }

    @Test
    @DisplayName("existing user — does NOT create a new user record")
    void onSuccess_existingUser_doesNotCreateNewUser() throws Exception {
        User existingUser = existingUser(EMAIL, NAME);
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        when(oAuth2User.getAttribute("name")).thenReturn(NAME);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn(JWT_TOKEN);

        handler.onAuthenticationSuccess(request, response, authentication);

        // save() should NOT be called for an existing user with a name
        verify(userRepository, never()).save(argThat(u -> u.getEmail().equals(EMAIL) && u.getAuthProvider() == null));
    }

    // -----------------------------------------------------------------------
    // New user flow
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("new user — creates user with GOOGLE provider and saves to DB")
    void onSuccess_newUser_createsUserWithGoogleProvider() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        when(oAuth2User.getAttribute("name")).thenReturn(NAME);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        User savedUser = existingUser(EMAIL, NAME);
        savedUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn(JWT_TOKEN);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Capture the user saved to DB
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User created = userCaptor.getValue();
        assertEquals(EMAIL, created.getEmail());
        assertEquals(NAME, created.getName());
        assertEquals(AuthProvider.GOOGLE, created.getAuthProvider());
        assertNull(created.getPassword(), "Google users must not have a password");
    }

    @Test
    @DisplayName("new user — redirects with JWT after creation")
    void onSuccess_newUser_redirectsWithJwt() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        when(oAuth2User.getAttribute("name")).thenReturn(NAME);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        User savedUser = existingUser(EMAIL, NAME);
        savedUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn(JWT_TOKEN);

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        assertTrue(redirectCaptor.getValue().contains("token=" + JWT_TOKEN));
    }

    // -----------------------------------------------------------------------
    // Name fallback: use given_name if name is null
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("new user — uses given_name when name attribute is null")
    void onSuccess_newUser_usesGivenNameWhenNameIsNull() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        when(oAuth2User.getAttribute("name")).thenReturn(null);          // name is null
        when(oAuth2User.getAttribute("given_name")).thenReturn("Bharadwaj"); // fallback
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        User savedUser = existingUser(EMAIL, "Bharadwaj");
        savedUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn(JWT_TOKEN);

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("Bharadwaj", userCaptor.getValue().getName());
    }

    // -----------------------------------------------------------------------
    // Missing email — must return 400
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("missing email from OAuth2 provider — sends 400 Bad Request")
    void onSuccess_missingEmail_sends400() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from OAuth2 provider");
        verify(response, never()).sendRedirect(any());
        verify(jwtService, never()).generateToken(any());
    }

    // -----------------------------------------------------------------------
    // Existing user with null name — should be updated
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("existing user with null name — name is updated and saved")
    void onSuccess_existingUserNullName_updatesName() throws Exception {
        User userWithNullName = existingUser(EMAIL, null); // name is null in DB
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        when(oAuth2User.getAttribute("name")).thenReturn(NAME);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithNullName));
        when(userRepository.save(userWithNullName)).thenReturn(userWithNullName);
        when(jwtService.generateToken(userWithNullName)).thenReturn(JWT_TOKEN);

        handler.onAuthenticationSuccess(request, response, authentication);

        // User should be saved with the updated name
        verify(userRepository).save(userWithNullName);
        assertEquals(NAME, userWithNullName.getName());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private User existingUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        return user;
    }
}
