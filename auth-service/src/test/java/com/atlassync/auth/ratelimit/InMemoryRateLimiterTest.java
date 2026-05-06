package com.atlassync.auth.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private final RateLimiter limiter = new InMemoryRateLimiter();

    @Test
    void allowsCallsUpToTheLimit() {
        assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void rejectsCallsBeyondTheLimit() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("k", 3, Duration.ofMinutes(1));
        }
        assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void countsAreScopedPerKey() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a", 3, Duration.ofMinutes(1));
        }
        assertThat(limiter.tryAcquire("b", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void releasesAfterWindowElapses() throws InterruptedException {
        Duration window = Duration.ofMillis(80);
        assertThat(limiter.tryAcquire("k", 1, window)).isTrue();
        assertThat(limiter.tryAcquire("k", 1, window)).isFalse();
        Thread.sleep(120);
        assertThat(limiter.tryAcquire("k", 1, window)).isTrue();
    }
}
