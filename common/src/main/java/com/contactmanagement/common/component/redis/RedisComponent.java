package com.contactmanagement.common.component.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.ConfigurableObjectInputStream;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisComponent {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public <T> T get(String key, Class<T> type) {
        try {
            String v = redis.opsForValue().get(key);
            return v == null ? null : mapper.readValue(v, type);
        } catch (Exception e) {
            log.warn("Redis get failed key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("Redis set failed key={}: {}", key, e.getMessage());
        }
    }

    public <T extends Serializable> T getObject(String key, Class<T> type) {
        try {
            String v = redis.opsForValue().get(key);
            if (v == null) return null;
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try (ConfigurableObjectInputStream in = new ConfigurableObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(v)), loader)) {
                Object value = in.readObject();
                return type.cast(value);
            }
        } catch (Exception e) {
            log.warn("Redis object get failed key={}: {}", key, e.getMessage());
            delete(key);
            return null;
        }
    }

    public void setObject(String key, Serializable value, Duration ttl) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ObjectOutputStream stream = new ObjectOutputStream(out)) {
                stream.writeObject(value);
            }
            redis.opsForValue().set(key, Base64.getEncoder().encodeToString(out.toByteArray()), ttl);
        } catch (Exception e) {
            log.warn("Redis object set failed key={}: {}", key, e.getMessage());
        }
    }

    public String getString(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis string get failed key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("Redis delete failed key={}: {}", key, e.getMessage());
        }
    }

    public long increment(String key) {
        try {
            Long v = redis.opsForValue().increment(key);
            return v == null ? 0 : v;
        } catch (Exception e) {
            log.warn("Redis increment failed key={}: {}", key, e.getMessage());
            return 0;
        }
    }
}