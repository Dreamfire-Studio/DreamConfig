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
/// Fired after a config file/document has been deleted.
/// </summary>
/// <remarks>
/// Triggered when <see cref="DreamConfigAPI.delete"/> completes.
/// Useful for cleanup or auditing when configs are removed.
/// </remarks>
/// <example>
/// <code>
/// @EventHandler
/// public void onConfigDeleted(ConfigDeletedEvent event) {
///     plugin.getLogger().info("Deleted config: " + event.getConfig().documentID());
/// }
/// </code>
/// </example>
public final class ConfigDeletedEvent extends ConfigEvent {
    /// <summary>
    /// Creates a new deletion event.
    /// </summary>
    /// <param name="cfg">The deleted config instance.</param>
    public ConfigDeletedEvent(IDreamConfig cfg) { super(cfg); }
}