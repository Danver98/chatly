package com.danver.messengerserver.configs.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

@Slf4j
public class CustomRedisCache extends RedisCache {
    protected CustomRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfiguration) {
        super(name, cacheWriter, cacheConfiguration);
    }

    @Override
    protected Object lookup(Object key) {
        try {
            return super.lookup(key);
        } catch (RedisConnectionFailureException ex) {
            log.info("RedisConnectionFailureException in RedisCache.lookup(): unable to connect to Redis Server");
            return null;
        }
    }

    @Override
    public void put(Object key, Object value) {
        try {
            super.put(key, value);;
        } catch (RedisConnectionFailureException ex) {
            log.info("RedisConnectionFailureException in RedisCache.put(): unable to connect to Redis Server");
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            return super.putIfAbsent(key, value);
        } catch (RedisConnectionFailureException ex) {
            log.info("RedisConnectionFailureException in RedisCache.putIfAbsent(): unable to connect to Redis Server");
            return null;
        }
    }

    @Override
    public void clear() {
        try {
            super.clear();
        } catch (RedisConnectionFailureException ex) {
            log.info("RedisConnectionFailureException in RedisCache.clear(): unable to connect to Redis Server");
        }
    }

    @Override
    public void evict(Object key) {
        try {
            super.evict(key);
        } catch (RedisConnectionFailureException ex) {
            log.info("RedisConnectionFailureException in RedisCache.evict(): unable to connect to Redis Server");
        }
    }
}
