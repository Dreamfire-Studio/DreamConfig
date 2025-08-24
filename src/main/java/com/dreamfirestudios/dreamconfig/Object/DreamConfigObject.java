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
package com.dreamfirestudios.dreamconfig.Object;

import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.DreamConfigFooter;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.DreamConfigHeader;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/// <summary>
/// Thin YAML-backed wrapper for a single config document.
/// </summary>
/// <remarks>
/// Handles first-time file creation, header/footer application, deep-section extraction,
/// and basic set/save/delete operations. Use alongside the YAML repository.
/// </remarks>
/// <example>
/// <code>
/// DreamConfigObject obj = new DreamConfigObject(plugin, "configs", "playerdata");
/// obj.set("stats.health", 20);
/// obj.save();
/// </code>
/// </example>
public final class DreamConfigObject {
    private final File file;
    private final FileConfiguration config;
    private final boolean firstLoad;

    /// <summary>
    /// Construct and load a YAML config file under the plugin's data folder.
    /// </summary>
    /// <param name="plugin">Owning plugin (defaults to <see cref="DreamConfig.get()"/> if null).</param>
    /// <param name="configPath">Relative path inside the plugin data folder.</param>
    /// <param name="documentId">File name (without extension).</param>
    public DreamConfigObject(JavaPlugin plugin, String configPath, String documentId) {
        if (plugin == null) plugin = DreamConfig.get();
        var dir = new File(plugin.getDataFolder(), configPath);
        if (!dir.exists() && !dir.mkdirs()) plugin.getLogger().severe("Could not create directory: " + dir.getPath());
        this.file = new File(dir, documentId + ".yml");
        this.firstLoad = !file.exists();
        if (firstLoad) firstCreate(plugin);
        this.config = YamlConfiguration.loadConfiguration(file);
        if (firstLoad) save();
    }

    /// <summary>True if the file was created on this load.</summary>
    public boolean isFirstLoad() { return firstLoad; }

    /// <summary>
    /// Return a deep map view of a configuration section.
    /// </summary>
    /// <param name="path">Section root path.</param>
    /// <returns>Recursive map of keys to values or nested maps.</returns>
    /// <example>
    /// <code>
    /// var map = obj.asDeepMap("my.config");
    /// </code>
    /// </example>
    public HashMap<Object, Object> asDeepMap(String path) {
        var out = new HashMap<Object, Object>();
        var section = config.getConfigurationSection(path);
        if (section == null) return out;
        for (var key : section.getKeys(false)) {
            var full = path + "." + key;
            if (config.isConfigurationSection(full)) out.put(key, asDeepMap(full));
            else out.put(key, config.get(full));
        }
        return out;
    }

    /// <summary>Set a YAML value at the given path.</summary>
    /// <param name="path">YAML path.</param>
    /// <param name="value">Value to store.</param>
    public void set(String path, Object value) { config.set(path, value); }

    /// <summary>Persist YAML to disk.</summary>
    public void save() {
        try { config.save(file); }
        catch (IOException e) { throw new RuntimeException("Failed to save YAML: " + file, e); }
    }

    /// <summary>Delete the YAML file (best-effort).</summary>
    public void delete() { if (!file.delete()) file.deleteOnExit(); }

    /// <summary>Apply a header block from <see cref="DreamConfigHeader"/>.</summary>
    /// <param name="header">Header annotation instance.</param>
    public void setHeader(DreamConfigHeader header) {
        var lines = new ArrayList<String>();
        lines.add("# +----------------------------------------------------+ #");
        lines.addAll(Arrays.asList(header.value()));
        lines.add("# +----------------------------------------------------+ #");
        config.options().setHeader(lines);
    }

    /// <summary>Apply a footer block from <see cref="DreamConfigFooter"/>.</summary>
    /// <param name="footer">Footer annotation instance.</param>
    public void setFooter(DreamConfigFooter footer) {
        var lines = new ArrayList<String>();
        lines.add("# +----------------------------------------------------+ #");
        lines.addAll(Arrays.asList(footer.value()));
        lines.add("# +----------------------------------------------------+ #");
        config.options().setFooter(lines);
    }

    /// <summary>
    /// Write version metadata under a root path (e.g., document id).
    /// </summary>
    /// <param name="rootPath">Root path (typically <c>cfg.documentID()</c>).</param>
    /// <param name="version">Version number to write.</param>
    public void setMetaVersion(String rootPath, int version) {
        set(rootPath + ".__meta", new LinkedHashMap<>());
        set(rootPath + ".__meta.version", version);
    }

    /// <summary>Create the file on first load; logs to console.</summary>
    private void firstCreate(JavaPlugin plugin) {
        Bukkit.getConsoleSender().sendMessage("Creating config for first time: " + file.getPath());
        try {
            if (!file.createNewFile()) plugin.getLogger().severe("Failed to create config: " + file.getPath());
        } catch (IOException e) {
            plugin.getLogger().severe("Error creating file " + file.getPath() + " -> " + e);
        }
    }
}