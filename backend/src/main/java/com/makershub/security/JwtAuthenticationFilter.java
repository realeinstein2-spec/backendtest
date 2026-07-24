package com.makershub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final com.makershub.repository.UserRepository userRepository;

    // BUG-05 fix: Bounded cache to prevent memory leaks, auto-cleared if exceeding 10,000 entries
    private final java.util.Map<UUID, Long> lastActiveCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && jwtUtil.isTokenValid(jwt, "ACCESS")) {
                UUID userId = jwtUtil.extractUserId(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(
                        jwtUtil.parseToken(jwt).get("phone", String.class));
                if (userDetails instanceof UserDetailsImpl impl && impl.getId().equals(userId) && impl.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    updateLastActiveOptimized(impl.getId());
                }
            }
        } catch (Exception ex) {
            log.warn("Could not set user authentication: {}", ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private void updateLastActiveOptimized(UUID userId) {
        long now = System.currentTimeMillis();
        if (lastActiveCache.size() > 10_000) {
            lastActiveCache.clear(); // Safety cap against memory leak
        }
        Long lastUpdated = lastActiveCache.get(userId);
        if (lastUpdated == null || (now - lastUpdated) > 60_000) { // 1 minute interval
            lastActiveCache.put(userId, now);
            try {
                userRepository.findById(userId).ifPresent(user -> {
                    user.setLastActiveAt(java.time.Instant.ofEpochMilli(now));
                    userRepository.save(user);
                });
            } catch (Exception e) {
                log.warn("Failed to update last active timestamp for user {}: {}", userId, e.getMessage());
            }
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
