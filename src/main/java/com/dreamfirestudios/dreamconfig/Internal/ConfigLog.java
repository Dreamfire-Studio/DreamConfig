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

import java.util.logging.Level;
import java.util.logging.Logger;

/// <summary>Centralized logger for the config subsystem.</summary>
/// <remarks>
/// Delegates to a provided base <see cref="Logger"/>. Must be initialized with <see cref="init(Logger)"/>.
/// </remarks>
/// <example>
/// <code>
/// ConfigLog.init(getLogger());
/// ConfigLog.info("Loaded config.");
/// </code>
/// </example>
public final class ConfigLog {
    private static Logger LOG;
    private ConfigLog() {}

    /// <summary>Initialize with a base <see cref="Logger"/>.</summary>
    public static void init(Logger base) { LOG = base; }

    /// <summary>Log informational message.</summary>
    public static void info(String s) { if (LOG != null) LOG.info(s); }

    /// <summary>Log warning message.</summary>
    public static void warn(String s) { if (LOG != null) LOG.warning(s); }

    /// <summary>Log error with throwable.</summary>
    public static void error(String s, Throwable t) { if (LOG != null) LOG.log(Level.SEVERE, s, t); }
}