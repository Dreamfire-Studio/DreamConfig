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
package com.dreamfirestudios.dreamconfig.Model;

import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/// <summary>
/// Base class for immutable-identity (“static”) configs stored under a single document id (default: class name).
/// </summary>
/// <typeparam name="T">Self type.</typeparam>
/// <remarks>
/// Only one instance exists per class.
/// Cache is maintained in <see cref="DreamConfig.STATIC_CACHE"/>.
/// </remarks>
public abstract class StaticPulseConfig<T extends StaticPulseConfig<T>> implements IDreamConfig {
    /// <summary>
    /// Get (or create) the single static instance for this class asynchronously.
    /// </summary>
    /// <param name="plugin">Owning plugin.</param>
    /// <param name="clazz">Config class type.</param>
    /// <param name="onSuccess">Callback with loaded instance.</param>
    /// <typeparam name="T">Config subclass type.</typeparam>
    /// <example>
    /// <code>
    /// StaticPulseConfig.getAsync(plugin, MyConfig.class, cfg -> {
    ///     System.out.println(cfg.documentID());
    /// });
    /// </code>
    /// </example>
    @SuppressWarnings("unchecked")
    public static <T extends StaticPulseConfig<T>> void getAsync(JavaPlugin plugin, Class<T> clazz, Consumer<T> onSuccess) {
        CompletableFuture.runAsync(() -> {
            var cached = (T) DreamConfig.STATIC_CACHE.get(clazz.getSimpleName());
            if (cached != null) {
                onSuccess.accept(cached);
                return;
            }
            try {
                T inst = clazz.getDeclaredConstructor().newInstance();
                DreamConfigAPI.load(plugin, inst, onSuccess)
                        .thenRun(() -> DreamConfig.STATIC_CACHE.put(inst.documentID(), inst));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create static config: " + clazz.getSimpleName(), e);
            }
        });
    }
}