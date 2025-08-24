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
package com.dreamfirestudios.dreamconfig.Internal;

import java.util.concurrent.*;

/// <summary>
/// Bounded thread-pool for config I/O tasks to avoid blocking the main thread.
/// </summary>
/// <remarks>
/// Provides async execution helpers backed by a small <see cref="ThreadPoolExecutor"/>.  
/// Core size = 1, max size = 2, queue size = 256. Threads are daemon and named "DreamConfig-IO".  
/// Excess tasks fall back to <see cref="ThreadPoolExecutor.CallerRunsPolicy"/>.
/// </remarks>
/// <example>
/// <code>
/// ConfigExecutor.init();
/// ConfigExecutor.runAsync(() -> { /* background work */ });
/// CompletableFuture&lt;String&gt; f = ConfigExecutor.supplyAsync(() -> "result");
/// </code>
/// </example>
public final class ConfigExecutor {
    private static ExecutorService EXEC;
    private ConfigExecutor() {}

    /// <summary>Initialize the thread pool if not already initialized.</summary>
    public static void init() {
        if (EXEC != null) return;
        EXEC = new ThreadPoolExecutor(
                1, 2, 30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),
                r -> {
                    var t = new Thread(r, "DreamConfig-IO");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /// <summary>Shut down the executor service gracefully.</summary>
    public static void shutdown() { if (EXEC != null) EXEC.shutdown(); }

    /// <summary>
    /// Submit a runnable to execute asynchronously.
    /// </summary>
    /// <param name="r">Task to run.</param>
    public static void runAsync(Runnable r) { EXEC.submit(r); }

    /// <summary>
    /// Submit a callable returning a value asynchronously.
    /// </summary>
    /// <param name="c">Callable task.</param>
    /// <typeparam name="T">Return type.</typeparam>
    /// <returns>CompletableFuture representing the result.</returns>
    public static <T> CompletableFuture<T> supplyAsync(Callable<T> c) {
        var f = new CompletableFuture<T>();
        EXEC.submit(() -> {
            try { f.complete(c.call()); }
            catch (Throwable t) { f.completeExceptionally(t); }
        });
        return f;
    }
}