package com.danver.messengerserver.configs.cache;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.util.Map;

public class CustomRedisCacheManager extends RedisCacheManager {

    public static CustomRedisCacheManager of(RedisCacheWriter cacheWriter, RedisCacheManagerBuilder builder) {
        RedisCacheManager manager = builder.build();
        return new CustomRedisCacheManager(cacheWriter, builder.cacheDefaults(),
                manager.isAllowRuntimeCacheCreation(), manager.getCacheConfigurations());
    }

    public CustomRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, boolean allowRuntimeCacheCreation, Map<String, RedisCacheConfiguration> initialCacheConfigurations) {
        super(cacheWriter, defaultCacheConfiguration, allowRuntimeCacheCreation, initialCacheConfigurations);
    }

    @Override
    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfiguration) {
        return new CustomRedisCache(name, this.getCacheWriter(),
                cacheConfiguration != null ? cacheConfiguration : this.getDefaultCacheConfiguration());
    }
}
