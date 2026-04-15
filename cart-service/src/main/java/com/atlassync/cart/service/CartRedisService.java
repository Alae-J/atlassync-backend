package com.atlassync.cart.service;

import com.atlassync.cart.dto.CartItemDto;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
public class CartRedisService {

    private final HashOperations<String, String, CartItemDto> hashOps;
    private final RedisTemplate<String, Object> redisTemplate;

    public CartRedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    public void putItem(String sessionId, String barcode, CartItemDto item) {
        hashOps.put(key(sessionId), barcode, item);
        redisTemplate.expire(key(sessionId), Duration.ofHours(24));
    }

    public Optional<CartItemDto> getItem(String sessionId, String barcode) {
        return Optional.ofNullable(hashOps.get(key(sessionId), barcode));
    }

    public Map<String, CartItemDto> getCart(String sessionId) {
        return hashOps.entries(key(sessionId));
    }

    public void removeItem(String sessionId, String barcode) {
        hashOps.delete(key(sessionId), barcode);
    }

    public void deleteCart(String sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private String key(String sessionId) {
        return "cart:" + sessionId;
    }
}
