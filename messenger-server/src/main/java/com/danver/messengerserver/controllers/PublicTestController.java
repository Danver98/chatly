package com.danver.messengerserver.controllers;

import com.danver.messengerserver.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * This is a temporal controller for test purposes only
 */
@Slf4j
@RestController()
@RequestMapping("/public")
public class PublicTestController {

    private final RedisTemplate<String, ?> redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PublicTestController(RedisTemplate<String, ?> redisTemplate, JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/long-task")
    public ResponseEntity<?> runLongTask() {
        try {
            long time = 3000L;
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    @PostMapping("/user-chats-redis/update")
    @GetMapping("/user-chats-redis/update")
    ResponseEntity<?> setUserChatsRedisInfo() {

        List<Map<String, Object>> usersChats = jdbcTemplate.queryForList("""
            select
                "userId",
                jsonb_agg("chatId")::text "chats"
            from
                "userschats"
            group by
                "userId"
        """);
        Map<String, String> usersChatsRedis = new HashMap<>();
        for (Map<String, Object> m: usersChats) {
            usersChatsRedis.put(Long.toString((Long) m.get("userId")), (String) m.get("chats"));
        }

        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        try {
            hashOps.putAll(Constants.REDIS_USERS_PERMISSIONS, usersChatsRedis);
        } catch (RedisConnectionFailureException ex) {
            log.info("Unable to connect to Redis server");
            // TODO: queue write to Redis later
        }
        return null;
    }
}
