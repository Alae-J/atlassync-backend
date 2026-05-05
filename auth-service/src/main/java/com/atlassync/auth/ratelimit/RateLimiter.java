package com.atlassync.auth.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    /**
     * Records a hit against {@code key} and reports whether it stays inside the rolling window.
     *
     * @return {@code true} when the call is permitted, {@code false} when the limit is exceeded.
     */
    boolean tryAcquire(String key, int max, Duration window);
}
