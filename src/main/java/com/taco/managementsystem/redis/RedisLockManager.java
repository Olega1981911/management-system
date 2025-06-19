package com.taco.managementsystem.redis;

import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisLockManager {
    private final RedisTemplate<String, String> redisTemplate;
    private final Duration defaultLockTimeout = Duration.ofSeconds(30);

    public boolean acquireLock(String lockKey, String lockId, Duration timeout) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                lockId,
                timeout != null ? timeout : defaultLockTimeout
        );
        return Boolean.TRUE.equals(result);
    }

    public void releaseLock(String lockKey, String lockId) {
        String currentLockId = redisTemplate.opsForValue().get(lockKey);
        if (lockId.equals(currentLockId)) {
            redisTemplate.delete(lockKey);
        }
    }
}
