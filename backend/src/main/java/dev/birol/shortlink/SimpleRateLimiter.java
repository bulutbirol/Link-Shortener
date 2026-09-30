package dev.birol.shortlink;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class SimpleRateLimiter {
    private record Window(long startsAt, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    boolean allow(String key, int max, long seconds) {
        long now = Instant.now().getEpochSecond();
        Window window = windows.compute(key, (ignored, old) ->
                old == null || now - old.startsAt >= seconds ? new Window(now, 1) : new Window(old.startsAt, old.count + 1));
        if (windows.size() > 20_000) windows.entrySet().removeIf(entry -> now - entry.getValue().startsAt > 3600);
        return window.count <= max;
    }
}
