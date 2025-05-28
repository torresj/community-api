package com.torresj.community.services.impl;

import com.torresj.community.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.token.secret}")
    private final String secret;

    @Value("${jwt.token.expiration}")
    private final String expiration;

    @Value("${jwt.token.prefix}")
    private final String prefix;

    @Value("${jwt.token.issuer.info}")
    private final String issuer;

    @Override
    public String createJWS(String name) {
        log.debug("[JWT SERVICE] Generating JWT");
        return Jwts.builder()
                .issuedAt(new Date())
                .issuer(issuer)
                .subject(name)
                .expiration(new Date(System.currentTimeMillis() + Long.parseLong(expiration)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Override
    public String validateJWS(String jws) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(jws);
            return claims.getPayload().getSubject();
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return null;
    }
}