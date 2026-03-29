package com.example.bank.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LockService {

    private final StringRedisTemplate redis;

    public boolean tryLock(String key) {
        Boolean success = redis.opsForValue()
                .setIfAbsent(key, "1", 5, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    public void release(String key) {
        redis.delete(key);
    }
}