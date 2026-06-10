package com.mphasis.tse.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RequestIdFilter — X-Request-Id propagation")
class RequestIdFilterTest {

    private RequestIdFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestIdFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("generates UUID when no X-Request-Id header present")
    void noHeader_generatesUuid() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Request-Id"), argThat(val -> val != null && val.length() == 36));
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("uses provided X-Request-Id when present")
    void headerPresent_usesIt() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("custom-id-123");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Request-Id", "custom-id-123");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("cleans up MDC after filter completes")
    void mdcCleanedUp() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("test-id");

        filter.doFilterInternal(request, response, chain);

        assertNull(MDC.get("requestId"), "MDC should be cleaned up after filter");
    }

    @Test
    @DisplayName("generates UUID when header is blank")
    void blankHeader_generatesUuid() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("   ");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Request-Id"), argThat(val -> !val.isBlank() && val.length() == 36));
    }
}
