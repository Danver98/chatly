package com.danver.messengerserver.configs.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.interceptor.CacheErrorHandler;

@Slf4j
public class RedisCacheErrorHandler implements CacheErrorHandler {
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.info("Unable to get from cache " + cache.getName() + " : " + exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.info("Unable to put into cache " + cache.getName() + " : " + exception.getMessage());
        this.evictUserDetailsCache(cache);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.info("Unable to evict from cache " + cache.getName() + " : " + exception.getMessage());
        this.evictUserDetailsCache(cache);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.info("Unable to clean cache " + cache.getName() + " : " + exception.getMessage());
        this.evictUserDetailsCache(cache);
    }

    @CacheEvict(value="userDetails", allEntries=true)
    private void evictUserDetailsCache(Cache cache) {
        log.info("Flushing 'userDetails' cache");
    }
}
