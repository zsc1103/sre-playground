package com.sre.playground.service.api;

import io.micrometer.core.instrument.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
public class WorkController {

    private final Map<String, byte[]> heapHog = new ConcurrentHashMap<>();
    private final AtomicLong heapBytesHeld = new AtomicLong(0);

    private final Timer workTimer;
    private final DistributionSummary heapAllocSummary;
    @SuppressWarnings("unused")
    private final Gauge heapHeldGauge;

    public WorkController(MeterRegistry registry) {
        this.workTimer = Timer.builder("demo_work_seconds")
                .description("/api/work total duration")
                .publishPercentileHistogram()
                .register(registry);

        this.heapAllocSummary = DistributionSummary.builder("demo_work_heap_alloc_bytes")
                .description("/api/work allocated heap bytes")
                .baseUnit("bytes")
                .register(registry);

        this.heapHeldGauge = Gauge.builder("demo_work_heap_held_bytes", heapBytesHeld, AtomicLong::get)
                .description("heap bytes currently held by /api/work")
                .baseUnit("bytes")
                .register(registry);
    }

    @GetMapping("/work")
    public Map<String, Object> work(
            @RequestParam(defaultValue = "0") long latencyMs,
            @RequestParam(defaultValue = "0") long cpuMs,
            @RequestParam(defaultValue = "0") int memMb,
            @RequestParam(defaultValue = "false") boolean hold,
            @RequestParam(required = false) String key
    ) {
        return workTimer.record(() -> {
            if (latencyMs > 0) sleep(latencyMs);
            if (cpuMs > 0) burnCpu(cpuMs);

            long allocatedBytes = 0;

            if (memMb > 0) {
                allocatedBytes = (long) memMb * 1024 * 1024;
                byte[] block = new byte[(int) allocatedBytes];

                for (int i = 0; i < block.length; i += 4096) {
                    block[i] = (byte) (i & 0xFF);
                }

                heapAllocSummary.record(allocatedBytes);

                if (hold) {
                    String realKey = (key == null || key.isBlank())
                            ? "hold-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000)
                            : key;

                    byte[] prev = heapHog.put(realKey, block);
                    if (prev != null) heapBytesHeld.addAndGet(-prev.length);
                    heapBytesHeld.addAndGet(block.length);

                    return Map.of(
                            "ok", true,
                            "key", realKey,
                            "heapHeldBytes", heapBytesHeld.get()
                    );
                }
            }

            return Map.of(
                    "ok", true,
                    "allocatedBytes", allocatedBytes,
                    "heapHeldBytes", heapBytesHeld.get()
            );
        });
    }

    @PostMapping("/work/free")
    public Map<String, Object> free(@RequestParam String key) {
        byte[] removed = heapHog.remove(key);
        if (removed != null) {
            heapBytesHeld.addAndGet(-removed.length);
            return Map.of("ok", true, "freedBytes", removed.length);
        }
        return Map.of("ok", false, "message", "not found");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private static void burnCpu(long cpuMs) {
        long end = System.nanoTime() + Duration.ofMillis(cpuMs).toNanos();
        long x = 0;
        while (System.nanoTime() < end) {
            x ^= (x << 1) + 0x9e3779b97f4a7c15L;
        }
        if (x == 42) System.out.print("");
    }
}
