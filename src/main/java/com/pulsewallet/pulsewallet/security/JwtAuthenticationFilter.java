package com.pulsewallet.pulsewallet.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads a {@code Authorization: Bearer <token>} header once per request and,
 * if the token is valid, populates the {@link SecurityContextHolder} so
 * downstream authorization checks see an authenticated user.
 *
 * <p>Registered in {@code SecurityConfig} ahead of
 * {@code UsernamePasswordAuthenticationFilter}. Requests with no header, a
 * malformed header, or an invalid/expired token simply continue
 * unauthenticated - {@code SecurityConfig}'s {@code authorizeHttpRequests}
 * rule is what actually rejects them with 401 via
 * {@link RestAuthenticationEntryPoint}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            String email = jwtService.extractUsername(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (jwtService.isValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
            // Malformed/expired token, or the user it names no longer exists:
            // leave the request unauthenticated rather than failing the filter
            // chain outright, so the 401 comes from one place
            // (RestAuthenticationEntryPoint) instead of two.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
