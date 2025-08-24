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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/// <summary>
/// Base Bukkit event for all DreamConfig lifecycle events.
/// </summary>
/// <remarks>
/// All config-related events extend this base class.
/// They are fired synchronously on the Bukkit main thread by <see cref="DreamConfigAPI"/>.
/// Use <see cref="getConfig()"/> to access the configuration instance associated with the event.
/// </remarks>
/// <example>
/// <code>
/// @EventHandler
/// public void onConfigLoaded(ConfigLoadedEvent event) {
///     IDreamConfig cfg = event.getConfig();
///     plugin.getLogger().info("Config loaded: " + cfg.documentID());
/// }
/// </code>
/// </example>
public abstract class ConfigEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final IDreamConfig config;

    /// <summary>
    /// Creates a new config event.
    /// </summary>
    /// <param name="config">Config instance associated with this event.</param>
    protected ConfigEvent(IDreamConfig config) {
        super(true); // marked async creation; still dispatched synchronously
        this.config = config;
    }

    /// <summary>
    /// Gets the config instance associated with this event.
    /// </summary>
    /// <returns>The config object that triggered the event.</returns>
    public IDreamConfig getConfig() { return config; }

    @Override public HandlerList getHandlers() { return HANDLERS; }

    /// <summary>
    /// Bukkit handler list accessor.
    /// </summary>
    public static HandlerList getHandlerList() { return HANDLERS; }
}