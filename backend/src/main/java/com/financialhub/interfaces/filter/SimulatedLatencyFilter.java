package com.financialhub.interfaces.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SimulatedLatencyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Simulated-Latency-Ms";
    private static final long MAX_MS = 30_000L;

    private final long delayMs;

    public SimulatedLatencyFilter(@Value("${app.latency.simulated-ms:0}") long delayMs) {
        this.delayMs = Math.min(Math.max(delayMs, 0L), MAX_MS);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (delayMs <= 0) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(HEADER_NAME, Long.toString(delayMs));
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        filterChain.doFilter(request, response);
    }
}
