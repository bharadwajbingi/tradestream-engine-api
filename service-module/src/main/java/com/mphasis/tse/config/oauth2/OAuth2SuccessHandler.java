package com.mphasis.tse.config.oauth2;

import com.mphasis.tse.entity.User;
import com.mphasis.tse.enums.AuthProvider;
import com.mphasis.tse.filter.JwtService;
import com.mphasis.tse.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${security.oauth2.frontend-redirect-url:http://localhost:5173/oauth2/redirect}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from OAuth2 provider");
            return;
        }

        String name = oAuth2User.getAttribute("name");
        if (name == null) {
            name = oAuth2User.getAttribute("given_name");
        }

        final String finalName = name;
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(finalName);
            newUser.setPassword(null);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            return userRepository.save(newUser);
        });

        if (user.getName() == null && finalName != null) {
            user.setName(finalName);
            user = userRepository.save(user);
        }

        String jwtToken = jwtService.generateToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", jwtToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
