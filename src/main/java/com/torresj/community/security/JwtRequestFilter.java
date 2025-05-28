package com.torresj.community.security;

import com.torresj.community.services.JwtService;
import com.torresj.community.services.UserService;
import com.torresj.community.services.impl.UserServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Value("${jwt.token.prefix}")
    private final String prefix;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain
    ) throws ServletException, IOException {
        // look for Bearer auth header
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(prefix + " ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = header.substring(7);
        final String name = jwtService.validateJWS(token);
        if (name == null) {
            // validation failed or token expired
            chain.doFilter(request, response);
            return;
        }

        // set user details on spring security context
        final UserDetails userDetails = ((UserServiceImpl) userService).loadUserByUsername(name);
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // continue with authenticated user
        chain.doFilter(request, response);
    }
}