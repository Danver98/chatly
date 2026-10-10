package com.danver.messengerserver.services.permission;

import com.danver.messengerserver.exceptions.CompletableFutureException;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.permission.Permission;
import com.danver.messengerserver.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PermissionRepository implements IPermissionRepository<UserDetails, Long> {

    private final RedisTemplate<String, ?> redis;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DataSource dataSource;

    private static final String PERMISSION_KEY = Constants.REDIS_USERS_PERMISSIONS;
    private static final int REDIS_BATCH_SIZE = 1000;
    private static final int CACHE_REFRESH_BATCH_SIZE = 2000;

    @Autowired
    public PermissionRepository(RedisTemplate<String, ?> redis, JdbcTemplate jdbcTemplate, DataSource dataSource,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    // ==================== PUBLIC API METHODS ====================

    @Override
    public List<String> getPermissions(UserDetails principal, Long resourceId, int resourceType) {
        long userId = ((User) principal).getId();
        String redisKey = buildRedisKey(userId, resourceId, resourceType);

        try {
            HashOperations<String, String, String> hashOps = redis.opsForHash();
            String permissionsStr = hashOps.get(PERMISSION_KEY, redisKey);

            if (permissionsStr != null && !permissionsStr.equals("[]")) {
                return deserializePermissions(permissionsStr);
            }

            // Cache miss - fetch from DB and cache
            List<String> permissions = getPermissionsFromDB(userId, resourceId, resourceType);
            if (permissions != null && !permissions.isEmpty()) {
                cachePermissionInRedis(redisKey, permissions);
            }
            return permissions;

        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis connection failed for user: {}, falling back to DB", userId);
            return getPermissionsFromDB(userId, resourceId, resourceType);
        }
    }

    @Override
    public int addPermission(UserDetails principal, Long resourceId, int resourceType, String permission) {
        return addPermissionsBatch(
                Collections.singletonList(principal),
                resourceId,
                resourceType,
                Collections.singletonList(permission)
        );
    }

    @Override
    public int addPermission(Long userId, Long resourceId, int resourceType, String permission) {
        UserDetails principal = User.builder().id(userId).build();
        return addPermission(principal, resourceId, resourceType, permission);
    }

    @Override
    public int addPermission(List<Long> userIds, Long resourceId, int resourceType, String permission) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        List<UserDetails> principals = userIds.stream()
                .map(userId -> User.builder().id(userId).build())
                .collect(Collectors.toList());

        return addPermissionsBatch(principals, resourceId, resourceType, Collections.singletonList(permission));
    }

    @Override
    public int addPermission(long[] userIds, Long resourceId, int resourceType, String permission) {
        if (userIds == null || userIds.length == 0) {
            return 0;
        }

        List<UserDetails> principals = Arrays.stream(userIds)
                .mapToObj(userId -> User.builder().id(userId).build())
                .collect(Collectors.toList());

        return addPermissionsBatch(principals, resourceId, resourceType, Collections.singletonList(permission));
    }

    @Override
    public int addPermission(long[] userIds, long resourceId, int resourceType, List<String> permissions) {
        if (userIds == null || userIds.length == 0) {
            return 0;
        }

        List<UserDetails> principals = Arrays.stream(userIds)
                .mapToObj(userId -> User.builder().id(userId).build())
                .collect(Collectors.toList());

        return addPermissionsBatch(principals, resourceId, resourceType, permissions);
    }

    /**
     * Delete permissions for multiple users for a specific resource
     *
     * @param userIds Array of user IDs
     * @param resource Resource ID (can be null for resource-independent permissions)
     * @param resourceType Resource type
     * @return Number of deleted records, or -1 if operation failed
     */
    @Override
    public int deletePermissions(long[] userIds, Long resource, int resourceType) {
        if (userIds == null || userIds.length == 0) {
            log.debug("Empty user IDs array, nothing to delete");
            return 0;
        }

        // Remove duplicates from userIds
        long[] distinctUserIds = Arrays.stream(userIds).distinct().toArray();
        // Convert to Long[] for PostgreSQL
        Long[] longUserIds = Arrays.stream(distinctUserIds)
                .boxed()
                .toArray(Long[]::new);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userIds", longUserIds);
        params.addValue("resource", resource);
        params.addValue("resourceType", resourceType);

        String sql = """
            delete from
                "UsersPermissions"
            where
                "user" = any(:userIds)
                and "resource" is not distinct from :resource
                and "resource_type" = :resourceType
            """;

        int deletedCount = namedParameterJdbcTemplate.update(sql, params);

        log.info("Deleted {} permission records for {} users, resource: {}, type: {}",
                deletedCount, distinctUserIds.length, resource, resourceType);

        // Also delete from Redis cache
        deletePermissionsFromRedis(distinctUserIds, resource, resourceType);

        return deletedCount;
    }

    @Override
    public int deletePermission(List<Long> users, Long resource, int resourceType, String permission) {
        if (users == null || users.isEmpty()) {
            log.debug("Empty user IDs array, nothing to delete");
            return 0;
        }
        long startTime = System.nanoTime();
        // Execute DB and Redis operations in parallel
        CompletableFuture<Integer> dbFuture = CompletableFuture.supplyAsync(() ->
                deletePermissionFromDB(users, resource, resourceType, permission));

        CompletableFuture<Integer> redisFuture = CompletableFuture.supplyAsync(() ->
                deletePermissionFromRedis(users, resource, resourceType, Collections.singletonList(permission)));

        try {
            CompletableFuture.allOf(dbFuture, redisFuture).join();
            int dbResult = dbFuture.get();
            int redisResult = redisFuture.get();
            if (redisResult < 0) {
                // TODO: add to queue for later update
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.info("Batch delete: {} users, {} permissions, {} ms | DB: {}, Redis: {}",
                    users.size(), 1, durationMs, dbResult, redisResult);

            return (dbResult < 0 || redisResult < 0) ? -1 : 0;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Batch delete failed for {} users", users.size(), e);
            throw new CompletableFutureException(e);
        }
    }

    /**
     * Batch add permissions for multiple users with multiple permissions
     * This is the most efficient method for bulk operations
     */
    @Transactional
    public int addPermissionsBatch(List<UserDetails> principals, Long resourceId, int resourceType, List<String> permissions) {
        if (principals == null || principals.isEmpty()) {
            log.debug("Empty principals list");
            return 0;
        }

        if (permissions == null || permissions.isEmpty()) {
            log.debug("Empty permissions list");
            return 0;
        }

        long startTime = System.nanoTime();

        // Deduplicate users by ID
        List<UserDetails> distinctPrincipals = new ArrayList<>(principals.stream()
                .collect(Collectors.toMap(
                        p -> ((User) p).getId(),
                        p -> p,
                        (existing, replacement) -> existing
                ))
                .values());

        // Deduplicate permissions
        List<String> distinctPermissions = permissions.stream()
                .distinct()
                .collect(Collectors.toList());

        // Execute DB and Redis operations in parallel
        CompletableFuture<Integer> dbFuture = CompletableFuture.supplyAsync(() ->
                addPermissionsToDB(distinctPrincipals, resourceId, resourceType, distinctPermissions));

        CompletableFuture<Integer> redisFuture = CompletableFuture.supplyAsync(() ->
                addPermissionsToRedis(distinctPrincipals, resourceId, resourceType, distinctPermissions));

        try {
            CompletableFuture.allOf(dbFuture, redisFuture).join();
            int dbResult = dbFuture.get();
            int redisResult = redisFuture.get();
            if (redisResult < 0) {
                // TODO: add to queue for later update
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.info("Batch add: {} users, {} permissions, {} ms | DB: {}, Redis: {}",
                    distinctPrincipals.size(), distinctPermissions.size(), durationMs, dbResult, redisResult);

            return (dbResult < 0 || redisResult < 0) ? -1 : 0;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Batch add failed for {} users", distinctPrincipals.size(), e);
            throw new CompletableFutureException(e);
        }
    }

    // ==================== DATABASE OPERATIONS ====================

    private List<String> getPermissionsFromDB(long userId, Long resourceId, int resourceType) {
        String sql = """
                    select
                        "permissions"
                    from
                        "UsersPermissions"
                    where
                        "user" = ?
                        and "resource" is not distinct from ?
                        and "resource_type" = ?
                    """;

        String[] permissionsArray = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Array array = rs.getArray("permissions");
            return array != null ? (String[]) array.getArray() : null;
        }, userId, resourceId, resourceType);

        return permissionsArray != null ? Arrays.asList(permissionsArray) : null;
    }

    @Transactional
    protected int addPermissionsToDB(List<UserDetails> principals, Long resourceId, int resourceType, List<String> permissions) {
        if (principals.isEmpty() || permissions.isEmpty()) {
            return 0;
        }
        // Extract user IDs as primitive long array
        long[] userIds = principals.stream()
                .mapToLong(p -> ((User) p).getId())
                .toArray();

        String[] permissionsArray = permissions.toArray(new String[0]);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userIds", userIds);
        params.addValue("resourceId", resourceId);
        params.addValue("resourceType", resourceType);
        params.addValue("permissions", permissionsArray);

        // Optimized single-query batch insert using UNNEST
        String sql = """
                    with input_data as (
                        select
                            unnest(:userIds) as user_id,
                            :resourceId as resource_id,
                            :resourceType as resource_type,
                            :permissions as new_permissions
                    ),
                    inserted as (
                        insert into
                            "UsersPermissions" ("user", "resource", "resource_type", "permissions")
                        select
                            id.user_id,
                            id.resource_id,
                            id.resource_type,
                            id.new_permissions
                        from
                            input_data id
                        on conflict
                            ("user", "resource", "resource_type")
                        do update
                        set
                            "permissions" = (
                                select array(
                                    select distinct unnest(
                                        coalesce("UsersPermissions"."permissions", array[]::text[]) ||
                                        excluded."permissions"
                                    )
                                    order by 1
                                )
                            )
                        returning
                            "user", "permissions"
                    )
                    select
                        count(*)
                    from
                        inserted
                    """;

        Integer updatedCount = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
        return updatedCount != null ? updatedCount : 0;
    }

    public int deletePermissionFromDB(List<Long> users, Long resource, int resourceType, String permission) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userIds", users.toArray(new Long[0]));
        params.addValue("resource", resource);
        params.addValue("resourceType", resourceType);
        params.addValue("permission", permission);

        String sql = """
            update
                "UsersPermissions"
            set
                "permissions" = array_remove("permissions", :permission::text)
            where
                "user" = any(:userIds)
                and "resource" is not distinct from :resource
                and "resource_type" = :resourceType
            """;

        return namedParameterJdbcTemplate.update(sql, params);
    }

    // ==================== REDIS OPERATIONS ====================

    private int addPermissionsToRedis(List<UserDetails> principals, Long resourceId, int resourceType, List<String> permissions) {
        if (principals.isEmpty() || permissions.isEmpty()) {
            return 0;
        }
        try {
            HashOperations<String, String, String> hashOps = redis.opsForHash();
            
            // Prepare keys
            List<String> keys = principals.stream()
                    .map(p -> buildRedisKey(((User) p).getId(), resourceId, resourceType))
                    .collect(Collectors.toList());

            // Batch get existing permissions
            List<String> existingPermissionsList = hashOps.multiGet(PERMISSION_KEY, keys);

            // Prepare updates
            Map<String, String> updates = new HashMap<>();
            int totalNewPermissions = 0;

            for (int i = 0; i < principals.size(); i++) {
                String key = keys.get(i);
                Set<String> allPermissions = getUniquePermissions(existingPermissionsList, i);

                int beforeSize = allPermissions.size();
                allPermissions.addAll(permissions);
                int addedCount = allPermissions.size() - beforeSize;

                if (addedCount > 0) {
                    String newValue = "[" + String.join(", ", allPermissions) + "]";
                    updates.put(key, newValue);
                    totalNewPermissions += addedCount;
                }
            }

            // Batch update only if there are changes
            if (!updates.isEmpty()) {
                // Process in batches of 1000
                Map<String, String> batchUpdates = new HashMap<>();
                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    batchUpdates.put(entry.getKey(), entry.getValue());
                    if (batchUpdates.size() >= REDIS_BATCH_SIZE) {
                        hashOps.putAll(PERMISSION_KEY, batchUpdates);
                        batchUpdates.clear();
                    }
                }
                if (!batchUpdates.isEmpty()) {
                    hashOps.putAll(PERMISSION_KEY, batchUpdates);
                }

                log.debug("Redis batch: updated {} keys, {} new permissions", updates.size(), totalNewPermissions);
            }

            return totalNewPermissions;

        } catch (RedisConnectionFailureException ex) {
            log.error("Redis connection failed for batch update", ex);
            return -1;
        } catch (Exception ex) {
            log.error("Unexpected Redis error", ex);
            return -1;
        }
    }

    private int deletePermissionFromRedis(List<Long> principals, Long resourceId, int resourceType, List<String> permissions) {
        if (principals.isEmpty() || permissions.isEmpty()) {
            return 0;
        }
        try {
            HashOperations<String, String, String> hashOps = redis.opsForHash();

            // Prepare keys
            List<String> keys = principals.stream()
                    .map(p -> buildRedisKey(p, resourceId, resourceType))
                    .collect(Collectors.toList());

            // Batch get existing permissions
            List<String> existingPermissionsList = hashOps.multiGet(PERMISSION_KEY, keys);

            // Prepare updates
            Map<String, String> updates = new HashMap<>();
            int totalDeletedPermissions = 0;

            for (int i = 0; i < principals.size(); i++) {
                String key = keys.get(i);
                Set<String> allPermissions = getUniquePermissions(existingPermissionsList, i);

                int beforeSize = allPermissions.size();
                permissions.forEach(allPermissions::remove);
                int deletedCount = beforeSize - allPermissions.size();

                if (deletedCount > 0) {
                    String newValue = "[" + String.join(", ", allPermissions) + "]";
                    updates.put(key, newValue);
                    totalDeletedPermissions += deletedCount;
                }
            }

            // Batch update only if there are changes
            if (!updates.isEmpty()) {
                // Process in batches of 1000
                Map<String, String> batchUpdates = new HashMap<>();
                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    batchUpdates.put(entry.getKey(), entry.getValue());
                    if (batchUpdates.size() >= REDIS_BATCH_SIZE) {
                        hashOps.putAll(PERMISSION_KEY, batchUpdates);
                        batchUpdates.clear();
                    }
                }
                if (!batchUpdates.isEmpty()) {
                    hashOps.putAll(PERMISSION_KEY, batchUpdates);
                }

                log.debug("Redis batch: updated {} keys, {} new permissions", updates.size(), totalDeletedPermissions);
            }
            return totalDeletedPermissions;
        } catch (RedisConnectionFailureException ex) {
            log.error("Redis connection failed for batch update", ex);
            return -1;
        } catch (Exception ex) {
            log.error("Unexpected Redis error", ex);
            return -1;
        }
    }

    private void cachePermissionInRedis(String key, List<String> permissions) {
        try {
            HashOperations<String, String, String> hashOps = redis.opsForHash();
            String serialized = serializePermissions(permissions);
            hashOps.put(PERMISSION_KEY, key, serialized);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Failed to cache permissions in Redis for key: {}", key);
        }
    }

    /**
     * Delete permissions from Redis cache for multiple users
     */
    private void deletePermissionsFromRedis(long[] userIds, Long resource, int resourceType) {
        if (userIds == null || userIds.length == 0) {
            return;
        }
        try {
            HashOperations<String, String, String> hashOps = redis.opsForHash();

            // Prepare keys for batch deletion
            String[] keys = Arrays.stream(userIds)
                    .mapToObj(userId -> buildRedisKey(userId, resource, resourceType))
                    .toList().toArray(String[]::new);

            // Delete from Redis in batch
            Long deletedCount = hashOps.delete(PERMISSION_KEY, keys);

            log.debug("Deleted {} permission entries from Redis cache", deletedCount);

        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis connection failed while deleting permissions cache: {}", ex.getMessage());
        }
    }

    // ==================== SCHEDULED CACHE REFRESH ====================

    @Scheduled(fixedDelay = 300000, initialDelay = 0)
    public void refreshRedisCache() {
        long startTime = System.nanoTime();
        log.info("Starting Redis permission cache refresh");

        JdbcTemplate template = new JdbcTemplate(this.dataSource);
        template.setFetchSize(CACHE_REFRESH_BATCH_SIZE);

        int offset = 0;
        int totalProcessed = 0;
        Map<String, String> batchData = new HashMap<>();

        try {
            while (true) {
                List<Permission> permissions = template.query(
                        """
                        select
                            "id",
                            "user",
                            "resource",
                            "resource_type",
                            "permissions"
                        from
                            "UsersPermissions"
                        order by
                            "id"
                        LIMIT
                            ?
                        OFFSET
                            ?
                        """,
                        this::mapPermission,
                        CACHE_REFRESH_BATCH_SIZE,
                        offset
                );

                if (permissions.isEmpty()) {
                    break;
                }

                for (Permission p : permissions) {
                    String key = p.user() + ":" + p.resource() + ":" + p.resourceType();
                    String value = serializePermissions(Arrays.asList(p.permissions()));
                    batchData.put(key, value);

                    // Flush in sub-batches to manage memory
                    if (batchData.size() >= REDIS_BATCH_SIZE) {
                        flushBatchToRedis(batchData);
                        batchData.clear();
                    }
                }

                totalProcessed += permissions.size();
                offset += CACHE_REFRESH_BATCH_SIZE;
                log.debug("Processed cache chunk: {} records", permissions.size());
            }

            // Flush remaining data
            if (!batchData.isEmpty()) {
                flushBatchToRedis(batchData);
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.info("Redis cache refresh completed: {} records in {} ms", totalProcessed, durationMs);

        } catch (Exception ex) {
            log.error("Failed to refresh Redis permission cache", ex);
        }
    }

    private void flushBatchToRedis(Map<String, String> data) {
        try {
            HashOperations<String, String, String> hashOps = redis.opsForHash();
            hashOps.putAll(PERMISSION_KEY, data);
        } catch (RedisConnectionFailureException ex) {
            log.error("Failed to flush batch to Redis", ex);
        }
    }

    // ==================== UTILITY METHODS ====================

    private Permission mapPermission(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("id");
        long user = rs.getLong("user");
        Long resource = rs.getObject("resource", Long.class);
        short resourceType = rs.getShort("resource_type");
        Array permissionsArray = rs.getArray("permissions");
        String[] permissions = permissionsArray != null ? (String[]) permissionsArray.getArray() : new String[0];
        return new Permission(id, user, resource, resourceType, permissions);
    }

    private String buildRedisKey(long userId, Long resourceId, int resourceType) {
        return userId + ":" + resourceId + ":" + resourceType;
    }

    private List<String> deserializePermissions(String permissionsStr) {
        if (permissionsStr == null || permissionsStr.length() <= 2) {
            return new ArrayList<>();
        }
        // Remove brackets: [READ, WRITE] -> READ, WRITE
        String content = permissionsStr.substring(1, permissionsStr.length() - 1);
        // Handle empty array case: "[]" already caught above, but just in case
        if (content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Split by comma and trim each permission
        return Arrays.stream(content.split(","))
                .map(String::trim)
                .filter(perm -> !perm.isEmpty())
                .collect(Collectors.toList());
    }

    private String serializePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(", ", permissions) + "]";
    }

    private Set<String> getUniquePermissions(List<String> existingPermissionsList, int i) {
        String existingStr = existingPermissionsList.get(i);

        // Parse existing permissions
        Set<String> allPermissions;
        if (existingStr == null || existingStr.equals("[]")) {
            allPermissions = new HashSet<>();
        } else {
            String content = existingStr.substring(1, existingStr.length() - 1);
            if (content.isBlank()) {
                allPermissions = new HashSet<>();
            } else {
                allPermissions = new HashSet<>(Arrays.asList(content.split(", ")));
            }
        }
        return allPermissions;
    }
}