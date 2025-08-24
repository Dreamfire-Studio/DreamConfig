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
package com.dreamfirestudios.dreamconfig.Internal.Repo;

import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/// <summary>
/// Abstraction over the persistence layer (e.g., YAML, Mongo).
/// </summary>
/// <remarks>
/// Implementations should be side‑effect free except for I/O. Methods may throw
/// exceptions which should be handled at the call site. Implementations must not
/// mutate behavior of <see cref="IDreamConfig"/> beyond serialization hooks.
/// </remarks>
/// <example>
/// <code>
/// ConfigRepository repo = new YamlConfigRepository();
/// repo.save(cfg, obj);
/// repo.load(cfg, obj);
/// </code>
/// </example>
public interface ConfigRepository {
    /// <summary>Persist the config to the backing store.</summary>
    /// <param name="cfg">Config instance to save.</param>
    /// <param name="obj">Repository object wrapper used by YAML path.</param>
    void save(IDreamConfig cfg, DreamConfigObject obj) throws Exception;

    /// <summary>Load the config from the backing store into <paramref name="cfg"/>.</summary>
    /// <param name="cfg">Target config to populate.</param>
    /// <param name="obj">Repository object wrapper used by YAML path.</param>
    void load(IDreamConfig cfg, DreamConfigObject obj) throws Exception;

    /// <summary>Resolve on-disk path (YAML) or logical path (other repos).</summary>
    /// <param name="cfg">Config instance.</param>
    /// <returns>Relative path segment for this config.</returns>
    String resolvePath(IDreamConfig cfg);

    /// <summary>Enumerate config files under a base directory (YAML only).</summary>
    /// <param name="plugin">Owning plugin.</param>
    /// <param name="baseDir">Base directory relative to plugin data folder.</param>
    /// <returns>List of files found (may be empty).</returns>
    List<File> listConfigFiles(JavaPlugin plugin, File baseDir);

    /// <summary>Map a file name to a document ID (YAML only).</summary>
    /// <param name="f">File to map.</param>
    /// <returns>Document ID string.</returns>
    String fileToDocumentId(File f);

    /// <summary>Render a value for console viewing.</summary>
    /// <param name="value">Value to render.</param>
    /// <param name="indent">Indent size (spaces).</param>
    /// <returns>Pretty string or <c>null</c> for null input.</returns>
    String renderConsole(Object value, int indent) throws Exception;

    /// <summary>Return a string of spaces for indentation.</summary>
    /// <param name="spaces">Number of spaces.</param>
    /// <returns>Indent string.</returns>
    String indent(int spaces);
}