package com.clearn.api.auth;

import com.clearn.api.config.InternalApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalTokenAuthenticationFilter extends OncePerRequestFilter {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final InternalApiProperties properties;

    public InternalTokenAuthenticationFilter(InternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isInternalApi(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String configuredToken = properties.internalToken();
        String requestToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (tokenMatches(configuredToken, requestToken)) {
            authenticateInternal();
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean tokenMatches(String configuredToken, String requestToken) {
        if (configuredToken == null || configuredToken.isBlank() || requestToken == null || requestToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void authenticateInternal() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private boolean isInternalApi(HttpServletRequest request) {
        return pathWithinApplication(request).startsWith("/api/internal/");
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
