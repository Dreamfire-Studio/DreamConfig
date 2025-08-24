/*
 * MIT License
 *
 * Copyright (c) 2025 Dreamfire Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dreamfirestudios.dreamconfig.Internal.Metrics;

import com.dreamfirestudios.dreamconfig.Internal.ConfigLog;

/// <summary>
/// Simple try-with-resources timer to log elapsed time in milliseconds.
/// </summary>
/// <remarks>
/// Use inside a try-with-resources block to measure performance of operations.  
/// Logs output with the configured <see cref="ConfigLog"/>.
/// </remarks>
/// <example>
/// <code>
/// try (var timer = new MetricsTimer("saveConfig")) {
///     repository.save(cfg);
/// }
/// // Logs: [DreamConfig] saveConfig took X ms
/// </code>
/// </example>
public final class MetricsTimer implements AutoCloseable {
    private final String label;
    private final long startNs;

    /// <summary>
    /// Creates a new timer and starts measurement.
    /// </summary>
    /// <param name="label">Label used in log output.</param>
    public MetricsTimer(String label) {
        this.label = label;
        this.startNs = System.nanoTime();
    }

    /// <summary>
    /// Closes the timer and logs elapsed milliseconds.
    /// </summary>
    @Override public void close() {
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        ConfigLog.info("[DreamConfig] " + label + " took " + ms + " ms");
    }
}