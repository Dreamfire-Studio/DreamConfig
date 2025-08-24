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
package com.dreamfirestudios.dreamconfig.Versioning;

import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/// <summary>
/// Registry of migrators keyed by config class.
/// </summary>
/// <remarks>
/// Migrators are stored per config type and ordered ascending by <see cref="ConfigMigrator.fromVersion"/>.
/// </remarks>
/// <example>
/// <code>
/// MigratorRegistry.register(MyCfg.class, new V1toV2());
/// List&lt;ConfigMigrator&lt;MyCfg&gt;&gt; chain = MigratorRegistry.get(MyCfg.class);
/// </code>
/// </example>
public final class MigratorRegistry {
    private static final Map<Class<? extends IDreamConfig>, List<ConfigMigrator<?>>> REGISTRY = new ConcurrentHashMap<>();
    private MigratorRegistry() {}

    /// <summary>Register a migrator for a config class.</summary>
    /// <param name="cfgClass">Config class key.</param>
    /// <param name="migrator">Migrator instance.</param>
    public static <T extends IDreamConfig> void register(Class<T> cfgClass, ConfigMigrator<T> migrator) {
        REGISTRY.computeIfAbsent(cfgClass, k -> new ArrayList<>()).add(migrator);
        REGISTRY.get(cfgClass).sort(Comparator.comparingInt(ConfigMigrator::fromVersion));
    }

    /// <summary>Get the ordered migrator list for a config class.</summary>
    /// <param name="cfgClass">Config class key.</param>
    /// <returns>Ordered list of migrators (possibly empty).</returns>
    @SuppressWarnings("unchecked")
    public static <T extends IDreamConfig> List<ConfigMigrator<T>> get(Class<T> cfgClass) {
        return (List<ConfigMigrator<T>>) (List<?>) REGISTRY.getOrDefault(cfgClass, List.of());
    }
}