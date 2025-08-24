package com.uade.bookybe.config;

import com.uade.bookybe.core.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT Authentication Filter that intercepts HTTP requests and validates JWT tokens.
 * This filter extracts the JWT token from the Authorization header, validates it,
 * and sets the authentication context for the current request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract Authorization header
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No Authorization header found or doesn't start with Bearer");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract token from header
            String token = jwtService.extractTokenFromHeader(authHeader);
            if (token == null) {
                log.debug("No token found in Authorization header");
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token
            if (!jwtService.validateToken(token)) {
                log.warn("Invalid JWT token provided");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user information from token
            String userId = jwtService.getUserIdFromToken(token);
            String email = jwtService.getEmailFromToken(token);

            if (userId == null || email == null) {
                log.warn("Could not extract user information from token");
                filterChain.doFilter(request, response);
                return;
            }

            // Check if user is already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create authentication token
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
                );

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId, // Principal (user ID)
                    null,   // Credentials (we don't store password)
                    authorities
                );

                // Set additional details
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                log.debug("Successfully authenticated user: {} with email: {}", userId, email);
            }

        } catch (Exception e) {
            log.error("Error processing JWT authentication", e);
            // Don't block the request, just log the error
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip JWT processing for public endpoints
        String path = request.getRequestURI();
        return path.equals("/sign-up") || 
               path.equals("/sign-in") || 
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/swagger-ui.html");
    }
}
