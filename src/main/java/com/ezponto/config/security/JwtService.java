package com.ezponto.config.security;

import com.ezponto.domain.conta.Conta;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.expiration-hours:8}")
    private long expirationHours;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Conta conta) {
        return Jwts.builder()
                .subject(conta.getEmail())
                .claim("role", conta.getRole().name())
                .claim("contaId", conta.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationHours * 3600 * 1000))
                .signWith(getKey())
                .compact();
    }

    public String extrairEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    public boolean tokenValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
