package com.contactmanagement.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@AutoConfiguration
@EnableCaching
public class RedisCacheConfig {
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    CacheManager cacheManager(RedisConnectionFactory factory) {
        var serializer = GenericJacksonJsonRedisSerializer.builder().enableUnsafeDefaultTyping().build();
        var config = RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues().serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }
}
