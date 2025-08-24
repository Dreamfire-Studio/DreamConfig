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

/// <summary>
/// Migrates a config instance from one version to the next.
/// </summary>
/// <typeparam name="T">Config type.</typeparam>
/// <remarks>
/// Implementations should be idempotent with respect to their <see cref="fromVersion"/>.
/// </remarks>
/// <example>
/// <code>
/// public final class V1toV2 implements ConfigMigrator&lt;MyCfg&gt;{
///   public int fromVersion(){ return 1; }
///   public int toVersion(){ return 2; }
///   public void migrate(MyCfg cfg){ cfg.setNewField("default"); }
/// }
/// </code>
/// </example>
public interface ConfigMigrator<T extends IDreamConfig> {
    /// <summary>Version this migrator starts from.</summary>
    int fromVersion();
    /// <summary>Version this migrator migrates to.</summary>
    int toVersion();
    /// <summary>Perform the migration in‑place.</summary>
    /// <param name="cfg">Config instance to mutate.</param>
    void migrate(T cfg) throws Exception;
}