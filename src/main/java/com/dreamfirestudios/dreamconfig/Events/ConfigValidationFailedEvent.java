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
/// Fired when validation fails after deserialization.
/// </summary>
/// <remarks>
/// Triggered by <see cref="DreamConfigAPI.load"/> or <see cref="DreamConfigAPI.loadAll"/>
/// when the assigned <see cref="ConfigValidator"/> reports errors.
/// Use <see cref="getMessage"/> to retrieve the validation failure reason.
/// </remarks>
/// <example>
/// <code>
/// @EventHandler
/// public void onValidationFailed(ConfigValidationFailedEvent event) {
///     plugin.getLogger().warning("Validation failed: " + event.getMessage());
/// }
/// </code>
/// </example>
public final class ConfigValidationFailedEvent extends ConfigEvent {
    private final String message;

    /// <summary>
    /// Creates a new validation failure event.
    /// </summary>
    /// <param name="cfg">Config that failed validation.</param>
    /// <param name="message">Error message returned by validator.</param>
    public ConfigValidationFailedEvent(IDreamConfig cfg, String message) {
        super(cfg);
        this.message = message;
    }

    /// <summary>
    /// Gets the validation error message.
    /// </summary>
    public String getMessage() { return message; }
}