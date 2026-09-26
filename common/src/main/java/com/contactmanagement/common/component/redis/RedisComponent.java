package com.contactmanagement.common.component.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
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
            warn("get", key, e);
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            warn("set", key, e);
        }
    }

    public <T extends Serializable> T getObject(String key, Class<T> type) {
        try {
            String v = redis.opsForValue().get(key);
            if (v == null) return null;
            try (var in = new ConfigurableObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(v)), Thread.currentThread().getContextClassLoader())) {
                return type.cast(in.readObject());
            }
        } catch (Exception e) {
            warn("object get", key, e);
            delete(key);
            return null;
        }
    }

    public void setObject(String key, Serializable value, Duration ttl) {
        try {
            var out = new ByteArrayOutputStream();
            try (var stream = new ObjectOutputStream(out)) {
                stream.writeObject(value);
            }
            redis.opsForValue().set(key, Base64.getEncoder().encodeToString(out.toByteArray()), ttl);
        } catch (Exception e) {
            warn("object set", key, e);
        }
    }

    public String getString(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            warn("string get", key, e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            warn("delete", key, e);
        }
    }

    public long increment(String key) {
        try {
            Long v = redis.opsForValue().increment(key);
            return v == null ? 0 : v;
        } catch (Exception e) {
            warn("increment", key, e);
            return 0;
        }
    }

    private void warn(String action, String key, Exception e) {
        log.warn("Redis {} failed key={}: {}", action, key, e.getMessage());
    }
}
