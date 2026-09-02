package com.researchflow.util;

import com.researchflow.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 根据用户ID生成JWT
     */
    public String generateToken(Long userId) {

        Date now = new Date();
        Date expirationTime = new Date(
                now.getTime() + jwtProperties.ttl()
        );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expirationTime)
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析JWT，获取用户ID
     */
    public Long getUserId(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }

    /**
     * 判断JWT是否有效
     */
    public boolean validateToken(String token) {

        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
