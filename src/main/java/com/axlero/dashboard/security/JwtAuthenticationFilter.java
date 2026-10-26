package com.axlero.dashboard.security;

import com.axlero.dashboard.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

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

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("No Bearer token on {} {}",
                    request.getMethod(), request.getRequestURI());
        } else {
            String token = header.substring(7);

            try {
                String username = jwtService.extractUsername(token);
                log.info("JWT parsed successfully for subject '{}'", username);

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    var optionalUser = userRepository.findByUsername(username);

                    if (optionalUser.isEmpty()) {
                        log.warn("JWT subject not found in user repository");
                    } else {
                        var user = optionalUser.get();

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
                            log.info("JWT authentication established");
                        } else {
                            log.warn("JWT token failed validation");
                        }
                    }
                }
            } catch (JwtException | IllegalArgumentException exception) {
                log.warn("JWT parsing failed: {}",
                        exception.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
