package com.axlero.dashboard.security;

import com.axlero.dashboard.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null) {
            String trimmedHeader = header.trim();
            if (trimmedHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = trimmedHeader.substring(7).trim();

                if (!token.isEmpty()) {
                    try {
                        String username = jwtService.extractUsername(token);

                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
                            userRepository.findByUsername(username).ifPresent(user -> {
                                if (jwtService.isTokenValid(token, user.getUsername())) {
                                    var authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    user.getUsername(),
                                                    null,
                                                    List.of(new SimpleGrantedAuthority(
                                                            "ROLE_" + user.getRole()))
                                            );

                                    SecurityContextHolder.getContext()
                                            .setAuthentication(authentication);
                                }
                            });
                        }
                    } catch (JwtException | IllegalArgumentException exception) {
                        SecurityContextHolder.clearContext();
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}