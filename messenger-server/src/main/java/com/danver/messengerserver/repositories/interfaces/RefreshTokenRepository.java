package com.danver.messengerserver.repositories.interfaces;

import com.danver.messengerserver.models.RefreshToken;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long>,
                                        ListPagingAndSortingRepository<RefreshToken, Long> {

    /**
     * Find a refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all refresh tokens for a specific user
     */
    Iterable<RefreshToken> findByUserId(Long userId);

    /**
     * Find ONE token for given user (for device-less tokens)
     * @param userId
     * @return
     */
    @Cacheable(value = "token", key = "#userId")
    @Query("SELECT \"token\" FROM \"RefreshTokens\" WHERE \"user\" = :userId AND \"device\" IS NULL")
    String findTokenByUserId(@Param("userId") Long userId);

    /**
     * Find ONE token for given user and device
     * @param userId
     * @param deviceId
     * @return
     */
    @Cacheable(value = "token", key = "#userId + ':' + #deviceId")
    @Query("SELECT \"token\" FROM \"RefreshTokens\" WHERE \"user\" = :userId AND \"device\" IS NOT DISTINCT FROM :deviceId")
    String findTokenByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    /**
     * Delete all refresh tokens for a specific user (all devices including NULL)
     */
    @Caching(evict = {
            @CacheEvict(value = "token", key = "#userId")
    })
    @Modifying
    @Query("DELETE FROM \"RefreshTokens\" WHERE \"user\" = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * Delete refresh token for a specific user and device (non-NULL device)
     */
    @Caching(evict = {
            @CacheEvict(value = "token", key = "#userId"),
            @CacheEvict(value = "token", key = "#userId + ':' + #deviceId")
    })
    @Modifying
    @Query("DELETE FROM \"RefreshTokens\" WHERE \"user\" = :userId AND \"device\" = :deviceId")
    void deleteByUserIdAndDevice(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    /**
     * Delete refresh token for a specific user with NULL device
     */
    @Caching(evict = {
            @CacheEvict(value = "token", key = "#userId")
    })
    @Modifying
    @Query("DELETE FROM \"RefreshTokens\" WHERE \"user\" = :userId AND \"device\" IS NULL")
    void deleteByUserIdAndNullDevice(@Param("userId") Long userId);

    /**
     * Check if a token exists for a specific user
     */
    boolean existsByUserIdAndToken(Long userId, String token);

    /**
     * Delete a token by user ID and token
     */
    @Modifying
    @Query("DELETE FROM \"RefreshTokens\" WHERE \"user\" = :userId AND token = :token")
    void deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);

    /**
     * Delete tokens in a batch
     * @param ids
     * @return
     */
    @Modifying
    @Query("DELETE FROM \"RefreshTokens\" WHERE id IN (:ids)")
    int deleteAllByIds(@Param("ids") List<Long> ids);

    /**
     * Rotating refresh token value
     */
    @Caching(evict = {
            @CacheEvict(value = "token", key = "#userId"),
            @CacheEvict(value = "token", key = "#userId + ':' + #deviceId")
    })
    @Modifying
    @Query("""
        update
            "RefreshTokens"
        set
            "token" = :newToken
        where
            "user" = :userId
            and "device" IS NOT DISTINCT FROM :deviceId
""")
    int rotateToken(@Param("userId") Long userId,
                    @Param("deviceId") String deviceId,
                    @Param("newToken") String newToken);

    /**
     * Updates user network status (online/ofline etc.)
     */
    @Modifying
    @Query("""
            update 
                "RefreshTokens" 
            set
                "networkStatus" = :networkStatus,
                "statusUpdateTime" = :statusUpdateTime
            where
                "user" = :userId
                and "device" IS NOT DISTINCT FROM :deviceId
    """)
    int updateNetworkStatus(@Param("userId") Long userId, @Param("deviceId") String deviceId,
                             @Param("networkStatus") short networkStatus,
                             @Param("statusUpdateTime") Instant statusUpdateTime);

    /**
     * Setting offline status for inactive users
     */
    @Modifying
    @Query("""
            update 
                "RefreshTokens"
            set
                "networkStatus" = :networkStatus,
                "statusUpdateTime" = now()
            where
                "statusUpdateTime" < now() - make_interval(secs => :threshold::double precision);
    """)
    int setOfflineStatusForInactiveUsers(@Param("networkStatus") short networkStatus, @Param("threshold") Long thresholdInSeconds);
}

