package com.contactmanagement.common.component.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisComponent {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T get(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return Objects.isNull(value) ? null : objectMapper.readValue(value, type);
        } catch (Exception exception) {
            warn("get", key, exception);
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception exception) {
            warn("set", key, exception);
        }
    }

    public <T extends Serializable> T getObject(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (Objects.isNull(value)) return null;
            try (var inputStream = new ConfigurableObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(value)), Thread.currentThread().getContextClassLoader())) {
                return type.cast(inputStream.readObject());
            }
        } catch (Exception exception) {
            warn("object get", key, exception);
            delete(key);
            return null;
        }
    }

    public void setObject(String key, Serializable value, Duration ttl) {
        try {
            var outputBuffer = new ByteArrayOutputStream();
            try (var objectOutputStream = new ObjectOutputStream(outputBuffer)) {
                objectOutputStream.writeObject(value);
            }
            redisTemplate.opsForValue().set(key, Base64.getEncoder().encodeToString(outputBuffer.toByteArray()), ttl);
        } catch (Exception exception) {
            warn("object set", key, exception);
        }
    }

    public String getString(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception exception) {
            warn("string get", key, exception);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            warn("delete", key, exception);
        }
    }

    public long increment(String key) {
        try {
            Long incrementedValue = redisTemplate.opsForValue().increment(key);
            return Objects.isNull(incrementedValue) ? 0 : incrementedValue;
        } catch (Exception exception) {
            warn("increment", key, exception);
            return 0;
        }
    }

    private void warn(String action, String key, Exception exception) {
        log.warn("Redis {} failed key={}: {}", action, key, exception.getMessage());
    }
}