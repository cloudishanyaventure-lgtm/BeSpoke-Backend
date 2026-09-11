package com.BeSpoke.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final com.BeSpoke.repository.UserRepository users;

    public JwtAuthFilter(JwtService jwtService, com.BeSpoke.repository.UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String email = claims.getSubject();
                // Authorization must follow today's role/company, not yesterday's token claims.
                users.findByEmail(email).filter(com.BeSpoke.entity.User::isActive)
                        .filter(u -> !u.getRole().isStaff() || com.BeSpoke.service.WorkHierarchy.activeColleague(u, u))
                        // A password reset evicts every session issued before it.
                        .filter(u -> u.getCredentialsChangedAt() == null
                                || claims.getIssuedAt() == null
                                || !claims.getIssuedAt().toInstant().isBefore(u.getCredentialsChangedAt()))
                        .ifPresent(user -> {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    email, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid token - continue unauthenticated; protected routes will return 401/403.
            }
        }
        filterChain.doFilter(request, response);
    }
}
