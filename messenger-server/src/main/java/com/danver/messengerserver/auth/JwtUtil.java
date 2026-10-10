package com.danver.messengerserver.auth;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.utils.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.danver.messengerserver.utils.Constants.JWT_TYPE_KEY;
import static com.danver.messengerserver.utils.Constants.REFRESH_TOKEN_TYPE;

@Slf4j
@Component
public class JwtUtil {

    private final Environment env;
    private final SecretKey secret;
    private final SecretKey accessTokenSecret;
    private final SecretKey refreshTokenSecret;

    @Autowired
    public JwtUtil(Environment env) {
        this.env = env;
        this.secret = Keys.hmacShaKeyFor(Objects.requireNonNull(env.getProperty("jwt.secret")).getBytes());
        //this.secret = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(env.getProperty("jwt.secret")));
        this.accessTokenSecret = Keys.hmacShaKeyFor(Objects.requireNonNull(env.getProperty("jwt.secret")).getBytes());
        this.refreshTokenSecret = Keys.hmacShaKeyFor(Objects.requireNonNull(env.getProperty("jwt.refresh-token.secret")).getBytes());
    }

    public JwtUtil(Environment env, String keyPropertyName) {
        this.env = env;
        this.secret = Keys.hmacShaKeyFor(Objects.requireNonNull(env.getProperty(keyPropertyName)).getBytes());
        this.accessTokenSecret = Keys.hmacShaKeyFor(Objects.requireNonNull(env.getProperty("jwt.secret")).getBytes());
        this.refreshTokenSecret = Keys.hmacShaKeyFor(Objects.requireNonNull(env.getProperty("jwt.refresh-token.secret")).getBytes());
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.USER_JWT_LOGIN_KEY, user.getEmail());
        String subject = Long.toString(user.getId());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .subject(subject)
                .expiration(new Date(System.currentTimeMillis() + Long.parseLong(Objects.requireNonNull(env.getProperty("jwt.exp-in-millis")))))
                .issuedAt(new Date())
                .issuer(env.getProperty("jwt.iss"))
                .signWith(secret)
                .compact();
    }

    public String generateAccessToken(User user) {
        String issuer = env.getProperty("jwt.iss");
        String subject = Long.toString(user.getId());
        Map<String, Object> claims = new HashMap<>();
        Long expirationMillis = Long.valueOf(Objects.requireNonNull(env.getProperty("jwt.exp-in-millis")));
        claims.put(Constants.USER_JWT_LOGIN_KEY, user.getEmail());
        return this.generateToken(claims, issuer, subject, expirationMillis, this.accessTokenSecret);
    }

    public String generateRefreshToken(User user) {
        String issuer = env.getProperty("jwt.iss");
        String subject = String.valueOf(user.getId());
        Long expirationMillis = Long.valueOf(Objects.requireNonNull(env.getProperty("jwt.refresh-token.exp-in-millis")));
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_TYPE_KEY, REFRESH_TOKEN_TYPE);
        claims.put(Constants.USER_JWT_LOGIN_KEY, user.getEmail());
        return this.generateToken(claims, issuer, subject, expirationMillis, this.refreshTokenSecret);
    }

    public String generateToken(Map<String, Object> claims, String issuer, String subject,
                                Long expirationMillis, SecretKey secret) {
        // Registered claims: "iss", "sub", "aud", "exp"
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .subject(subject)
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .issuedAt(new Date())
                .issuer(issuer)
                .signWith(secret)
                .compact();
    }

    public String generateToken(Map<String, Object> claims, String issuer, String subject,
                                Long expirationMillis, String secret) {
        // Registered claims: "iss", "sub", "aud", "exp"
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .subject(subject)
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .issuedAt(new Date())
                .issuer(issuer)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    public String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            log.info("Checking token {} for validity", token);
            Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parse(token);
            return true;
        } catch (SecurityException | ExpiredJwtException | UnsupportedJwtException | MalformedJwtException |
                 SignatureException | IllegalArgumentException e) {
            log.info("Token is invalid: " + e.getMessage());
            return false;
        }
    }

    public Claims validateAndParse(String token, SecretKey secret) {
        return (Claims) Jwts.parser()
                .verifyWith(secret)
                .build()
                .parse(token)
                .getPayload();
    }

    /**
     * @param token
     * @return claims even if token is expired
     */
    public Claims validateAndParseAccessTokenIfExpired(String token) {
        try {
            return (Claims) Jwts.parser()
                    .verifyWith(accessTokenSecret)
                    .build()
                    .parse(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * @param token
     * @return claims even if token is expired
     */
    public Claims validateAndParseRefreshTokenIfExpired(String token) {
        try {
            return (Claims) Jwts.parser()
                    .verifyWith(refreshTokenSecret)
                    .build()
                    .parse(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     *
     * @param token
     * @return claims for access token
     */
    public Claims getClaims(String token) {
        return (Claims) Jwts.parser()
                .verifyWith(secret)
                .build()
                .parse(token)
                .getPayload();
    }
}
