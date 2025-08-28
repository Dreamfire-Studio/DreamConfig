package com.dreamfirestudios.dreamconfig.Metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <summary>Lightweight in-process metrics registry: counters, gauges, and timers.</summary>
 * <remarks>Backend-agnostic; you can export periodically or bridge to another system later.</remarks>
 */
public final class MetricsRegistry {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gauges   = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timersNs = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timerCount = new ConcurrentHashMap<>();

    private static final MetricsRegistry INSTANCE = new MetricsRegistry();
    private MetricsRegistry() {}

    /// <summary>Global singleton.</summary>
    public static MetricsRegistry get() { return INSTANCE; }

    /// <summary>Increment a counter by delta (can be negative).</summary>
    public void inc(String name, long delta) {
        counters.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(delta);
    }

    /// <summary>Read a counter.</summary>
    public long counter(String name) {
        return counters.getOrDefault(name, new AtomicLong()).get();
    }

    /// <summary>Set a gauge to an absolute value.</summary>
    public void setGauge(String name, long value) {
        gauges.computeIfAbsent(name, k -> new AtomicLong()).set(value);
    }

    /// <summary>Read a gauge.</summary>
    public long gauge(String name) {
        return gauges.getOrDefault(name, new AtomicLong()).get();
    }

    /// <summary>Record a timer sample in nanoseconds.</summary>
    public void recordNanos(String name, long nanos) {
        timersNs.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(nanos);
        timerCount.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
    }

    /// <summary>Total recorded time in milliseconds.</summary>
    public long timerTotalMs(String name) {
        return TimeUnit.NANOSECONDS.toMillis(timersNs.getOrDefault(name, new AtomicLong()).get());
    }

    /// <summary>Number of timer samples.</summary>
    public long timerCount(String name) {
        return timerCount.getOrDefault(name, new AtomicLong()).get();
    }
}