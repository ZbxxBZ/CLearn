package com.clearn.api.rate;

import com.clearn.api.auth.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class SubmissionRateLimitFilter extends OncePerRequestFilter {
    private static final String KEY_PREFIX = "clearn:submission-rate:";
    private static final String RATE_LIMIT_BODY = """
            {"success":false,"data":null,"message":"submission rate limit exceeded"}""";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int maxRequests;
    private final Duration window;

    public SubmissionRateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${clearn.submission.rate-limit.enabled:true}") boolean enabled,
            @Value("${clearn.submission.rate-limit.max-requests:3}") int maxRequests,
            @Value("${clearn.submission.rate-limit.window:PT10S}") Duration window
    ) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("clearn.submission.rate-limit.max-requests must be positive");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("clearn.submission.rate-limit.window must be positive");
        }
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || !isSubmissionRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        CurrentUser currentUser = currentUser();
        if (currentUser == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long count = incrementCounter(currentUser);
        if (count > maxRequests) {
            writeRateLimitedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSubmissionRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = pathWithinApplication(request);
        return path.matches("/api/problems/\\d+/submissions")
                || path.matches("/api/exams/\\d+/problems/\\d+/submissions");
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            return null;
        }
        return user;
    }

    private long incrementCounter(CurrentUser currentUser) {
        String key = KEY_PREFIX + currentUser.id();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count == null ? 1L : count;
    }

    private void writeRateLimitedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(RATE_LIMIT_BODY);
    }
}
