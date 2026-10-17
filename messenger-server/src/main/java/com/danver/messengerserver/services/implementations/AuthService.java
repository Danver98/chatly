package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.auth.AuthDTO;
import com.danver.messengerserver.auth.AuthData;
import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.exceptions.AppAuthenticationException;
import com.danver.messengerserver.models.RefreshToken;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.repositories.interfaces.RefreshTokenRepository;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.utils.Constants;
import com.danver.messengerserver.utils.NetworkStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.danver.messengerserver.utils.Constants.ACCESS_TOKEN_BLACKLIST_KEY;

@Slf4j
@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final Environment env;

    private final UserService userService;
    private final RefreshTokenRepository tokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int REFRESH_TOKEN_BATCH_SIZE = 2000;
    private static final int ACCESS_TOKEN_BLACKLIST_BATCH_SIZE = 1000;

    @Autowired
    public AuthService(JwtUtil jwtUtil, AuthenticationManager authenticationManager, Environment env, UserService userService, RefreshTokenRepository tokenRepository, RedisTemplate<String, String> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.env = env;
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.redisTemplate = redisTemplate;
    }

    public AuthData login(AuthDTO authDTO) {
        User user = userService.getUserByEmail(authDTO.getLogin());
        if (user == null) {
            throw new BadCredentialsException("Cannot find user got login %s".formatted(authDTO.getLogin()));
        }
        String existingRefreshToken = tokenRepository.findTokenByUserIdAndDeviceId(user.getId(), authDTO.getDeviceId());
        if (existingRefreshToken == null) {
            // Check password
            authenticationManager.authenticate
                    (new UsernamePasswordAuthenticationToken(authDTO.getLogin(), authDTO.getPassword()));
        } else {
            log.warn("user %d is already logged".formatted(user.getId()));
        }
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        int updated = tokenRepository.rotateToken(user.getId(), authDTO.getDeviceId(), refreshToken);
        if (updated == 0) {
            tokenRepository.save(new RefreshToken(user.getId(), refreshToken, authDTO.getDeviceId()));
        }
        return AuthData.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(HttpServletResponse response, String accessToken, String refreshToken, String deviceId) {
        // Delete refresh token form db
        Claims refreshClaims = jwtUtil.validateAndParseRefreshTokenIfExpired(refreshToken);
        long userId = Long.parseLong(refreshClaims.getSubject());
        tokenRepository.deleteByUserIdAndDevice(userId, deviceId);
        // Add access token to blacklist
        Claims claims = jwtUtil.validateAndParseAccessTokenIfExpired(accessToken);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        if (expiration.after(now)) {
            // Note: Redis supports hash field expiration only since 7.4 version.
            redisTemplate.opsForHash().put(ACCESS_TOKEN_BLACKLIST_KEY, accessToken, "1");
            long millisToExpire = expiration.getTime() - now.getTime();
            try {
                redisTemplate.opsForHash().expire(ACCESS_TOKEN_BLACKLIST_KEY, Duration.ofMillis(millisToExpire), List.of(accessToken));
            } catch (Exception e) {
                log.info("Couldn't set TTL for user %d, device id %s and access token %s due to the problem: %s"
                        .formatted(userId, deviceId, accessToken, e.getMessage()));
            }
        }
        SecurityContextHolder.getContext().setAuthentication(null);
        if (response.containsHeader(HttpHeaders.AUTHORIZATION)) {
            response.setHeader(HttpHeaders.AUTHORIZATION, null);
        }
    }

    public AuthData getNewRefreshToken(String refreshToken, String deviceId) {
        Claims claims = jwtUtil.validateAndParseRefreshTokenIfExpired(refreshToken);
        long userId = Long.parseLong(claims.getSubject());

        String dbRefreshToken = this.tokenRepository.findTokenByUserIdAndDeviceId(userId, deviceId);
        if (dbRefreshToken == null || !dbRefreshToken.equals(refreshToken)) {
            throw new AppAuthenticationException("Wrong refresh token provided for user %d".formatted(userId));
        }
        User user = (User) userService.loadUserByUsername((String) claims.get(Constants.USER_JWT_LOGIN_KEY));
        String accessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        // Update old token value with a new one
        int updated = tokenRepository.rotateToken(userId, deviceId, newRefreshToken);
        if (updated == 0) {
            tokenRepository.save(new RefreshToken(userId, newRefreshToken, deviceId));
        }
        return AuthData.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public Object updateNetworkStatus(User user, String deviceId, short status) {
        Instant statusUpdateTime = Instant.now();
        return this.tokenRepository.updateNetworkStatus(user.getId(), deviceId, status, statusUpdateTime);
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 0) // hourly
    public void deleteCorruptedRefreshTokens() {
        log.info("Starting deletion of corrupted refresh tokens");
        int page = 0;
        int totalDeleted = 0;
        int totalProcessed = 0;
        boolean hasNext = true;
        List<Long> idsToDelete = new ArrayList<>();

        while (hasNext) {
            Pageable pageable = PageRequest.of(page, REFRESH_TOKEN_BATCH_SIZE);
            Page<RefreshToken> refreshTokensPage = tokenRepository.findAll(pageable);

            List<RefreshToken> refreshTokens = refreshTokensPage.getContent();
            totalProcessed += refreshTokens.size();

            // Find invalid tokens
            for (RefreshToken token : refreshTokens) {
                try {
                    if (!jwtUtil.validateToken(token.getToken())) {
                        idsToDelete.add(token.getId());
                    }
                } catch (Exception e) {
                    idsToDelete.add(token.getId());
                }
            }

            // Batch delete if we have accumulated enough IDs
            if (idsToDelete.size() >= REFRESH_TOKEN_BATCH_SIZE) {
                totalDeleted += batchDeleteTokens(idsToDelete);
                idsToDelete.clear();
            }

            hasNext = refreshTokensPage.hasNext();
            page++;
        }

        // Delete remaining IDs
        if (!idsToDelete.isEmpty()) {
            totalDeleted += batchDeleteTokens(idsToDelete);
        }

        log.info("Completed deletion. Processed: {}, Deleted: {}", totalProcessed, totalDeleted);
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 0) // hourly
    public void deleteAccessTokensFromBlacklist() {
        log.info("Starting deletion of access tokens from blacklist");
        int totalDeleted = 0;
        int totalProcessed = 0;
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        ScanOptions options = ScanOptions.scanOptions()
                .count(ACCESS_TOKEN_BLACKLIST_BATCH_SIZE)
                .build();
        HashSet<String> invalidTokens = new HashSet<>();
        try(Cursor<Map.Entry<String, String>> cursor = hashOps.scan(ACCESS_TOKEN_BLACKLIST_KEY, options)) {
            while (cursor.hasNext()) {
                totalProcessed +=1;
                String accessToken = cursor.next().getKey();
                if (accessToken != null) {
                    boolean isValid = jwtUtil.validateToken(accessToken);
                    if (!isValid) {
                        invalidTokens.add(accessToken);
                    }
                }
                if (invalidTokens.size() == ACCESS_TOKEN_BLACKLIST_BATCH_SIZE) {
                    // Delete tokens if set is big enough
                    String [] keys = invalidTokens.toArray(String[]::new);
                    hashOps.delete(ACCESS_TOKEN_BLACKLIST_KEY, keys);
                    totalDeleted += invalidTokens.size();
                    invalidTokens.clear();
                }
            }
            if (!invalidTokens.isEmpty()) {
                String [] keys = invalidTokens.toArray(String[]::new);
                hashOps.delete(ACCESS_TOKEN_BLACKLIST_KEY, keys);
                totalDeleted += invalidTokens.size();
            }
        }
        log.info("Completed deletion. Processed: {}, Deleted: {}", totalProcessed, totalDeleted);
    }

    @Async
    @Scheduled(fixedRateString ="${application.user.network-status-check-period:25000}" )
    public void logoutInactiveUsers() {
        log.info("Starting \"Logout inactive users\" task");
        Long threshold = Long.parseLong(Objects.requireNonNull(this.env.getProperty("application.user.log-out-inactive-period"))) / 1000;
        tokenRepository.setOfflineStatusForInactiveUsers((short) NetworkStatus.OFFLINE.ordinal(),
                (short)NetworkStatus.ONLINE.ordinal(), threshold);
        log.info("Finished \"Logout inactive users\" task");
    }
    /**
     * Batch delete tokens by IDs
     */
    private int batchDeleteTokens(List<Long> ids) {
        if (ids.isEmpty()) return 0;
        return tokenRepository.deleteAllByIds(ids);
    }
}
