package com.sre.playground.service.api;

import io.micrometer.core.instrument.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api")
public class ErrorController {

    private final AtomicReference<Double> errorRate = new AtomicReference<>(0.0);

    private final Counter errorCounter;
    @SuppressWarnings("unused")
    private final Gauge errorRateGauge;

    public ErrorController(MeterRegistry registry) {
        this.errorCounter = Counter.builder("demo_error_total")
                .description("/api/error 5xx count")
                .register(registry);

        this.errorRateGauge = Gauge.builder("demo_error_rate", errorRate, AtomicReference::get)
                .description("current /api/error error rate (0~1)")
                .register(registry);
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> error(@RequestParam(required = false) Double rate) {
        double r = (rate != null) ? clamp(rate) : errorRate.get();
        boolean fail = ThreadLocalRandom.current().nextDouble() < r;

        if (fail) {
            errorCounter.increment();
            return ResponseEntity.status(500).body(Map.of(
                    "ok", false,
                    "error", "injected",
                    "rateUsed", r
            ));
        }

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "rateUsed", r
        ));
    }

    @PostMapping("/error/config")
    public Map<String, Object> setRate(@RequestParam double rate) {
        double v = clamp(rate);
        errorRate.set(v);
        return Map.of("ok", true, "errorRate", v);
    }

    private static double clamp(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
