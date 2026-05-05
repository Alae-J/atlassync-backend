package com.atlassync.auth.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnMissingBean(RateLimiter.class)
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int max, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst().isBefore(cutoff)) {
                queue.pollFirst();
            }
            if (queue.size() >= max) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }
}
