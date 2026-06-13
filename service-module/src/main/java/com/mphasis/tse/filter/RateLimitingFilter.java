package com.mphasis.tse.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.requests-per-minute:120}")
    private int requestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURI().startsWith("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientKey(request);
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            long nowMinute = Instant.now().getEpochSecond() / 60;
            if (existing == null || existing.minute != nowMinute) {
                return new Bucket(nowMinute);
            }
            return existing;
        });

        if (bucket.count.incrementAndGet() > requestsPerMinute) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Bucket {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger(0);

        private Bucket(long minute) {
            this.minute = minute;
        }
    }
}
