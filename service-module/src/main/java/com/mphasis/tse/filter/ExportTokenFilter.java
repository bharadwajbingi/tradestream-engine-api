package com.mphasis.tse.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mphasis.tse.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTokenFilter extends OncePerRequestFilter {

    private final ExportTokenService exportTokenService;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ((path.endsWith("/export") || path.contains("/export/")) 
            && !path.contains("/export/jobs") 
            && !path.contains("/export/status")) {
            String token = request.getHeader("X-Export-Token");
            if (token == null || token.isBlank()) {
                token = request.getParameter("token");
            }

            // Check if user is already authenticated via JWT
            var existingAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth != null && existingAuth.getPrincipal() instanceof com.mphasis.tse.entity.User) {
                com.mphasis.tse.entity.User userObj = (com.mphasis.tse.entity.User) existingAuth.getPrincipal();
                if (!userObj.isTotpEnabled()) {
                    log.info("Bypassing export token check for user {} since TOTP/2FA is not enabled", userObj.getEmail());
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            if (token == null || token.isBlank()) {
                log.warn("Missing export token for path: {}", path);
                writeErrorResponse(response, "Export token is missing. Please verify TOTP code first.");
                return;
            }

            String email = exportTokenService.validateExportTokenAndGetEmail(token);
            if (email == null) {
                log.warn("Invalid or expired export token for path: {}", path);
                writeErrorResponse(response, "Invalid or expired export token. Please verify TOTP code again.");
                return;
            }

            if (existingAuth != null && !email.equals(existingAuth.getName())) {
                log.warn("Export token subject {} does not match authenticated user {} for path: {}",
                        email, existingAuth.getName(), path);
                writeErrorResponse(response, "Export token does not belong to the current user. Please verify TOTP code again.");
                return;
            }

            log.info("Valid export token verified for user: {} on path: {}", email, path);

            if (existingAuth == null) {
                var user = userDetailsService.loadUserByUsername(email);
                var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        ApiResponse<Void> apiResponse = new ApiResponse<>("FORBIDDEN", 403, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
