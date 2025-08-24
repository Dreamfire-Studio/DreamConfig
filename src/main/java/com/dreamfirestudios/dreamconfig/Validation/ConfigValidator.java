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
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.dreamfirestudios.dreamconfig.Validation;

import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;

/// <summary>
/// Validator for a specific config type.
/// </summary>
/// <typeparam name="T">Config type to validate.</typeparam>
/// <remarks>
/// Return <c>null</c> to indicate “valid”, otherwise a human‑readable error message.
/// </remarks>
/// <example>
/// <code>
/// public final class MyCfgValidator implements ConfigValidator&lt;MyCfg&gt;{
///   public String validate(MyCfg instance){ return instance.maxPlayers() &gt; 0 ? null : "maxPlayers must be &gt; 0"; }
/// }
/// </code>
/// </example>
public interface ConfigValidator<T extends IDreamConfig> {
    /// <summary>Validate an instance.</summary>
    /// <param name="instance">Config instance.</param>
    /// <returns><c>null</c> if valid; otherwise an error message.</returns>
    String validate(T instance) throws Exception;
}