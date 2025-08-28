package com.dreamfirestudios.dreamconfig.Metrics;

import java.util.concurrent.TimeUnit;

public final class MetricsTimer implements AutoCloseable {
    private final String name;
    private final long startNanos;

    private MetricsTimer(String name) {
        this.name = name;
        this.startNanos = System.nanoTime();
    }

    /// <summary>Start a new timer for the given metric name.</summary>
    public static MetricsTimer start(String name) {
        return new MetricsTimer(name);
    }

    /// <summary>Stop and record the elapsed time into the MetricsRegistry.</summary>
    @Override
    public void close() {
        long elapsed = System.nanoTime() - startNanos;
        MetricsRegistry.get().recordNanos(name, elapsed);
    }

    /// <summary>Utility: record an explicit duration.</summary>
    public static void record(String name, long duration, TimeUnit unit) {
        MetricsRegistry.get().recordNanos(name, unit.toNanos(duration));
    }
}