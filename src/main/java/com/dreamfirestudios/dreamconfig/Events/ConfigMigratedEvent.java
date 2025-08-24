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
package com.dreamfirestudios.dreamconfig.Events;

import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;

/// <summary>
/// Fired when a config has been migrated from one version to another.
/// </summary>
/// <remarks>
/// Triggered during <see cref="DreamConfigAPI.load"/> or <see cref="DreamConfigAPI.loadAll"/>
/// if the config file version is lower than the target version.
/// Provides both <see cref="getFromVersion"/> and <see cref="getToVersion"/> for auditing.
/// </remarks>
/// <example>
/// <code>
/// @EventHandler
/// public void onConfigMigrated(ConfigMigratedEvent event) {
///     plugin.getLogger().info("Migrated " + event.getConfig().documentID() +
///         " from v" + event.getFromVersion() + " to v" + event.getToVersion());
/// }
/// </code>
/// </example>
public final class ConfigMigratedEvent extends ConfigEvent {
    private final int fromVersion;
    private final int toVersion;

    /// <summary>
    /// Creates a new migration event.
    /// </summary>
    /// <param name="cfg">The config being migrated.</param>
    /// <param name="fromVersion">Source version before migration.</param>
    /// <param name="toVersion">Target version after migration.</param>
    public ConfigMigratedEvent(IDreamConfig cfg, int fromVersion, int toVersion) {
        super(cfg);
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
    }

    /// <summary>Gets the version the config was migrated from.</summary>
    public int getFromVersion() { return fromVersion; }

    /// <summary>Gets the version the config was migrated to.</summary>
    public int getToVersion() { return toVersion; }
}