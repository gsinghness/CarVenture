package com.carventure.webapp.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        String token = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        if (token != null && validateToken(token)) {
            Authentication authentication = getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

    private boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("b2F8c7eR3t6Qp0zL9vX5gY4hK1jN8sM7wP0oU4fH2kR9aD6cQ3tY8zV4wP5eF7gU".getBytes()) // Use your actual signing key
                    .build()
                    .parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date()); // Check expiration
        } catch (Exception e) {
            return false; // Token is invalid
        }
    }

    private Authentication getAuthentication(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey("b2F8c7eR3t6Qp0zL9vX5gY4hK1jN8sM7wP0oU4fH2kR9aD6cQ3tY8zV4wP5eF7gU".getBytes()) // Use your actual signing key
                    .build()
                    .parseClaimsJws(token);

            String phoneNumber = claims.getBody().getSubject();
            return new UsernamePasswordAuthenticationToken(phoneNumber, null, null); // No authorities
        } catch (Exception e) {
            return null; // If there's an issue with getting the authentication
        }
    }
}
