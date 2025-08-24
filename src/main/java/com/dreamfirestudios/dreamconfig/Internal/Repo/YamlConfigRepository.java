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

import com.dreamfirestudios.dreamconfig.Internal.Reflection.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamconfig.Internal.Reflection.DreamConfigSerializer;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.StoragePath;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamcore.DreamFile.DreamDir;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/// <summary>
/// YAML-backed repository implementation (default).
/// </summary>
/// <remarks>
/// Uses <see cref="DreamConfigSerializer"/> and <see cref="DreamConfigDeSerializer"/> to handle all
/// reflection and hook invocation. The filesystem path is resolved via <see cref="StoragePath"/> or
/// <see cref="IDreamConfig.useSubFolder()"/>/<see cref="IDreamConfig.documentID()"/>.
/// </remarks>
/// <example>
/// <code>
/// YamlConfigRepository repo = new YamlConfigRepository();
/// DreamConfigObject obj = new DreamConfigObject("my-id");
/// repo.save(cfg, obj);
/// repo.load(cfg, obj);
/// </code>
/// </example>
public final class YamlConfigRepository implements ConfigRepository {

    /// <summary>Save via YAML serializer.</summary>
    public void save(IDreamConfig cfg, DreamConfigObject obj) throws Exception { DreamConfigSerializer.save(cfg, obj); }

    /// <summary>Load via YAML deserializer.</summary>
    public void load(IDreamConfig cfg, DreamConfigObject obj) throws Exception { DreamConfigDeSerializer.load(cfg, obj); }

    /// <summary>
    /// Resolve the relative path for this config on disk.
    /// </summary>
    /// <param name="cfg">Config instance.</param>
    /// <returns>Relative path segment.</returns>
    public String resolvePath(IDreamConfig cfg) {
        if (cfg.getClass().isAnnotationPresent(StoragePath.class))
            return cfg.getClass().getAnnotation(StoragePath.class).value();
        return cfg.useSubFolder() ? cfg.documentID() : "";
    }

    /// <summary>
    /// List all <c>.yml</c> files within <paramref name="baseDir"/> under the plugin data folder.
    /// </summary>
    /// <param name="plugin">Owning plugin.</param>
    /// <param name="baseDir">Relative base directory.</param>
    /// <returns>List of <c>.yml</c> files, or empty if none.</returns>
    public List<File> listConfigFiles(JavaPlugin plugin, File baseDir) {
        var root = new File(plugin.getDataFolder(), baseDir.getPath());
        if (!root.exists()) return List.of();
        var all = DreamDir.returnAllFilesFromDirectory(root, false);
        return all.stream().filter(f -> f.getName().endsWith(".yml")).collect(Collectors.toList());
    }

    /// <summary>Strip <c>.yml</c> extension to get document id.</summary>
    public String fileToDocumentId(File f) {
        var n = f.getName();
        return n.endsWith(".yml") ? n.substring(0, n.length() - 4) : n;
    }

    /// <summary>
    /// Render a value recursively in a simple YAML-like console format.
    /// </summary>
    /// <param name="value">Value to render.</param>
    /// <param name="indent">Indent size (spaces).</param>
    /// <returns>Pretty string or <c>null</c> for null input.</returns>
    public String renderConsole(Object value, int indent) throws Exception {
        if (value == null) return null;
        if (value instanceof Map<?, ?> map) return renderMap(map, indent);
        if (value instanceof List<?> list) return renderList(list, indent);
        return " ".repeat(Math.max(0, indent)) + value;
    }

    /// <summary>Return indentation string.</summary>
    public String indent(int spaces) { return " ".repeat(Math.max(0, spaces)); }

    /// <summary>Render a map with recursion.</summary>
    private String renderMap(Map<?, ?> map, int indent) throws Exception {
        if (map.isEmpty()) return "{}";
        var sb = new StringBuilder("{");
        for (var k : map.keySet()) {
            var rk = renderConsole(k, indent);
            var rv = renderConsole(map.get(k), indent);
            sb.append("\n").append(indent(indent)).append(rk).append(":").append(rv);
        }
        sb.append("\n").append(indent(indent - 1)).append("}");
        return sb.toString();
    }

    /// <summary>Render a list with recursion.</summary>
    private String renderList(List<?> list, int indent) throws Exception {
        if (list.isEmpty()) return "[]";
        var sb = new StringBuilder("[");
        for (var v : list) {
            var rv = renderConsole(v, indent);
            sb.append("\n").append(indent(indent)).append(rv);
        }
        sb.append("\n").append(indent(indent - 1)).append("]");
        return sb.toString();
    }
}