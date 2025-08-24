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
package com.dreamfirestudios.dreamconfig.API;

import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.Events.*;
import com.dreamfirestudios.dreamconfig.Internal.ConfigExecutor;
import com.dreamfirestudios.dreamconfig.Internal.ConfigLog;
import com.dreamfirestudios.dreamconfig.Internal.Metrics.MetricsTimer;
import com.dreamfirestudios.dreamconfig.Internal.Reflection.SerializerHelpers;
import com.dreamfirestudios.dreamconfig.Internal.Repo.ConfigRepository;
import com.dreamfirestudios.dreamconfig.Internal.Repo.YamlConfigRepository;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamconfig.Validation.ConfigValidator;
import com.dreamfirestudios.dreamconfig.Validation.ValidateWith;
import com.dreamfirestudios.dreamconfig.Versioning.ConfigVersion;
import com.dreamfirestudios.dreamconfig.Versioning.MetadataKeys;
import com.dreamfirestudios.dreamconfig.Versioning.MigratorRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/// <summary>
/// High-level entry point for DreamConfig operations: async save/load/delete,
/// migration, validation, and event dispatch.
/// </summary>
/// <remarks>
/// This API is designed for plugin developers who want to persist configuration
/// objects implementing <see cref="IDreamConfig"/>.  
/// Operations are dispatched asynchronously where possible, with synchronous Bukkit events 
/// fired on the main server thread.  
/// Validation and version migrations are built-in, reducing boilerplate in config classes.
/// </remarks>
/// <example>
/// <code>
/// // Example config class
/// @ConfigVersion(2)
/// public class MyConfig implements IDreamConfig {
///     private String name = "Default";
///     public String documentID() { return "myconfig"; }
/// }
///
/// // Usage in plugin
/// DreamConfigAPI.load(this, new MyConfig(), cfg -> {
///     getLogger().info("Loaded: " + cfg.getName());
/// });
/// </code>
/// </example>
public final class DreamConfigAPI {

    private DreamConfigAPI() {}

    private static volatile ConfigRepository REPO = new YamlConfigRepository();

    /// <summary>
    /// Saves a configuration object asynchronously.
    /// </summary>
    /// <typeparam name="T">Type of configuration object.</typeparam>
    /// <param name="plugin">Owning plugin instance.</param>
    /// <param name="cfg">Config instance to save.</param>
    /// <param name="onSuccess">Callback executed upon successful save.</param>
    /// <remarks>
    /// Writes both the config data and version metadata.  
    /// Triggers <see cref="ConfigSavedEvent"/> on completion.
    /// </remarks>
    public static <T extends IDreamConfig> void save(JavaPlugin plugin, T cfg, Consumer<T> onSuccess) {
        ConfigExecutor.runAsync(() -> {
            try (var timer = new MetricsTimer("save[" + cfg.documentID() + "]")) {
                var obj = new DreamConfigObject(plugin, REPO.resolvePath(cfg), cfg.documentID());
                if (obj.isFirstLoad()) cfg.FirstLoadConfig();

                REPO.save(cfg, obj);

                int ver = cfg.getClass().isAnnotationPresent(ConfigVersion.class)
                        ? cfg.getClass().getAnnotation(ConfigVersion.class).value()
                        : 1;
                obj.set(cfg.documentID() + "." + MetadataKeys.META_ROOT, new LinkedHashMap<>());
                obj.set(cfg.documentID() + "." + MetadataKeys.META_VERSION, ver);
                obj.save();

                cfg.AfterSaveConfig();
                dispatchSync(new ConfigSavedEvent(cfg));
                onSuccess.accept(cfg);
            } catch (Exception e) {
                throw new RuntimeException("Save failed for " + cfg.documentID(), e);
            }
        });
    }

    /// <summary>
    /// Gets the currently active repository implementation.
    /// </summary>
    public static ConfigRepository getRepository() { return REPO; }

    /// <summary>
    /// Overrides the repository implementation.
    /// </summary>
    /// <param name="repo">New repository to use (e.g., JSON instead of YAML).</param>
    public static void setRepository(ConfigRepository repo) {
        if (repo != null) REPO = repo;
    }

    /// <summary>
    /// Loads a configuration object asynchronously.
    /// </summary>
    /// <typeparam name="T">Type of configuration object.</typeparam>
    /// <param name="plugin">Owning plugin instance.</param>
    /// <param name="cfg">Config instance to load into.</param>
    /// <param name="onSuccess">Callback executed with the loaded config.</param>
    /// <returns>A future completing when the load finishes.</returns>
    /// <remarks>
    /// Handles first-load initialization, migration, and validation.  
    /// Fires <see cref="ConfigLoadedEvent"/>, <see cref="ConfigMigratedEvent"/>, or 
    /// <see cref="ConfigValidationFailedEvent"/> as appropriate.
    /// </remarks>
    public static <T extends IDreamConfig> CompletableFuture<Void> load(JavaPlugin plugin, T cfg, Consumer<T> onSuccess) {
        return ConfigExecutor.supplyAsync(() -> {
            try (var timer = new MetricsTimer("load[" + cfg.documentID() + "]")) {
                var obj = new DreamConfigObject(plugin, REPO.resolvePath(cfg), cfg.documentID());

                if (obj.isFirstLoad()) {
                    cfg.FirstLoadConfig();
                    REPO.save(cfg, obj);
                    writeVersionMeta(cfg, obj);
                    cfg.AfterSaveConfig();
                } else {
                    cfg.BeforeLoadConfig();
                    REPO.load(cfg, obj);

                    int diskVer = readVersionMeta(obj, cfg);
                    int targetVer = cfg.getClass().isAnnotationPresent(ConfigVersion.class)
                            ? cfg.getClass().getAnnotation(ConfigVersion.class).value()
                            : 1;

                    if (diskVer < targetVer) {
                        runMigrations(cfg, diskVer, targetVer);
                        REPO.save(cfg, obj);
                        writeVersionMeta(cfg, obj);
                        cfg.AfterSaveConfig();
                    }

                    String err = runValidation(cfg);
                    if (err != null) {
                        dispatchSync(new ConfigValidationFailedEvent(cfg, err));
                        ConfigLog.warn("[DreamConfig] Validation failed for " + cfg.documentID() + ": " + err);
                    }

                    cfg.AfterLoadConfig();
                }

                dispatchSync(new ConfigLoadedEvent(cfg));
                onSuccess.accept(cfg);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Load failed for " + cfg.documentID(), e);
            }
        });
    }

    /// <summary>
    /// Deletes a configuration file asynchronously.
    /// </summary>
    /// <typeparam name="T">Type of configuration object.</typeparam>
    /// <param name="plugin">Owning plugin instance.</param>
    /// <param name="cfg">Config instance representing the file to delete.</param>
    /// <param name="onSuccess">Callback executed after deletion.</param>
    /// <remarks>
    /// Triggers <see cref="ConfigDeletedEvent"/> on completion.
    /// </remarks>
    public static <T extends IDreamConfig> void delete(JavaPlugin plugin, T cfg, Consumer<T> onSuccess) {
        ConfigExecutor.runAsync(() -> {
            try (var timer = new MetricsTimer("delete[" + cfg.documentID() + "]")) {
                var obj = new DreamConfigObject(plugin, REPO.resolvePath(cfg), cfg.documentID());
                obj.delete();
                dispatchSync(new ConfigDeletedEvent(cfg));
                onSuccess.accept(cfg);
            }
            catch (Exception e) { throw new RuntimeException("Delete failed for " + cfg.documentID(), e); }
        });
    }

    /// <summary>
    /// Renders a config object to console for debugging.
    /// </summary>
    /// <typeparam name="T">Type of configuration object.</typeparam>
    /// <param name="cfg">Config instance to display.</param>
    /// <param name="onSuccess">Callback executed after display.</param>
    /// <remarks>
    /// Uses reflection to print all fields and values in YAML-like format.
    /// </remarks>
    public static <T extends IDreamConfig> void display(T cfg, Consumer<T> onSuccess) {
        ConfigExecutor.runAsync(() -> {
            try {
                var sb = new StringBuilder(String.format("==========[%s / PULSE CONFIG]==========\n{\n", cfg.documentID()));
                var data = SerializerHelpers.reflectAllFields(cfg.getClass(), cfg);
                for (var field : data.keySet()) {
                    var name = SerializerHelpers.resolveSaveName(field);
                    var rendered = REPO.renderConsole(data.get(field), 2);
                    if (rendered == null) continue;
                    sb.append(String.format("%s%s:%s\n", REPO.indent(1), name, rendered));
                }
                sb.append("}\n==========[END]==========");
                Bukkit.getConsoleSender().sendMessage(sb.toString());
                onSuccess.accept(cfg);
            } catch (Exception e) {
                throw new RuntimeException("Display failed for " + cfg.documentID(), e);
            }
        });
    }

    /// <summary>
    /// Loads all config files of a given type asynchronously.
    /// </summary>
    /// <param name="plugin">Owning plugin instance.</param>
    /// <param name="typeMarker">Marker instance of the config type.</param>
    /// <returns>Future resolving to a map of document IDs to config instances.</returns>
    /// <remarks>
    /// Applies migration and validation per file.  
    /// Useful for bulk-loading player configs or similar multi-instance setups.
    /// </remarks>
    public static CompletableFuture<Map<String, IDreamConfig>> loadAll(JavaPlugin plugin, IDreamConfig typeMarker) {
        return ConfigExecutor.supplyAsync(() -> {
            try (var timer = new MetricsTimer("loadAll[" + typeMarker.getClass().getSimpleName() + "]")) {
                var results = new HashMap<String, IDreamConfig>();
                var base = REPO.resolvePath(typeMarker);
                var files = REPO.listConfigFiles(plugin, new File(base));
                for (var file : files) {
                    var name = REPO.fileToDocumentId(file);
                    var inst = SerializerHelpers.createInstanceWithId(name, typeMarker.getClass());
                    if (inst == null) continue;
                    var cfg = (IDreamConfig) inst;
                    REPO.load(cfg, new DreamConfigObject(plugin, base, name));

                    int diskVer = readVersionMeta(new DreamConfigObject(plugin, base, name), cfg);
                    int targetVer = cfg.getClass().isAnnotationPresent(ConfigVersion.class)
                            ? cfg.getClass().getAnnotation(ConfigVersion.class).value()
                            : 1;
                    if (diskVer < targetVer) {
                        runMigrations(cfg, diskVer, targetVer);
                        REPO.save(cfg, new DreamConfigObject(plugin, base, name));
                        writeVersionMeta(cfg, new DreamConfigObject(plugin, base, name));
                    }
                    String err = runValidation(cfg);
                    if (err != null) {
                        dispatchSync(new ConfigValidationFailedEvent(cfg, err));
                    }

                    results.put(name, cfg);
                }
                return results;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
    }

    /* --------------------------- helpers --------------------------- */

    /// <summary>
    /// Dispatches a Bukkit event on the main thread.
    /// </summary>
    private static void dispatchSync(org.bukkit.event.Event event) {
        Bukkit.getScheduler().runTask(DreamConfig.get(), () -> Bukkit.getPluginManager().callEvent(event));
    }

    /**
     * Reads the __meta.version from the YAML tree produced by DreamConfigObject#asDeepMap.
     *
     * <p>Notes:
     * - Avoids Map#getOrDefault with mismatched default types (which triggers generics errors).
     * - Accepts version as Number or String; falls back to 1 if missing/invalid.
     * - Works even if "__meta" section is absent or wrongly typed.
     *
     * @param obj DreamConfigObject backing the file
     * @param cfg The config instance (used for document root)
     * @return parsed version, or 1 if not present/invalid
     */
    private static int readVersionMeta(DreamConfigObject obj, IDreamConfig cfg) {
        // asDeepMap returns a raw-ish map (Object,Object). Be defensive.
        final Map<Object, Object> root = obj.asDeepMap(cfg.documentID());
        if (root == null || root.isEmpty()) return 1;

        final Object metaObj = root.get("__meta");
        if (!(metaObj instanceof Map<?, ?> meta)) return 1;

        final Object verObj = meta.get("version");
        if (verObj == null) return 1;

        if (verObj instanceof Number n) return n.intValue();

        try {
            return Integer.parseInt(String.valueOf(verObj));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    /// <summary>
    /// Writes version metadata for a config.
    /// </summary>
    private static void writeVersionMeta(IDreamConfig cfg, DreamConfigObject obj) {
        int ver = cfg.getClass().isAnnotationPresent(ConfigVersion.class)
                ? cfg.getClass().getAnnotation(ConfigVersion.class).value()
                : 1;
        obj.setMetaVersion(cfg.documentID(), ver);
        obj.save();
    }

    /// <summary>
    /// Runs registered migrations from a starting version to a target version.
    /// </summary>
    /// <typeparam name="T">Type of configuration object.</typeparam>
    /// <param name="cfg">Config instance to migrate.</param>
    /// <param name="from">Starting version.</param>
    /// <param name="to">Target version.</param>
    private static <T extends IDreamConfig> void runMigrations(T cfg, int from, int to) throws Exception {
        var list = MigratorRegistry.get((Class<T>) cfg.getClass());
        int current = from;
        for (var m : list) {
            if (m.fromVersion() == current && m.toVersion() == current + 1) {
                try (var timer = new MetricsTimer("migrate[" + cfg.documentID() + " " + current + "->" + (current + 1) + "]")) {
                    m.migrate(cfg);
                }
                current++;
                dispatchSync(new ConfigMigratedEvent(cfg, m.fromVersion(), m.toVersion()));
                if (current == to) break;
            }
        }
        if (current != to) {
            ConfigLog.warn("[DreamConfig] Missing migrators to reach version " + to + " from " + from + " for " + cfg.getClass().getSimpleName());
        }
    }

    /// <summary>
    /// Runs validation if <see cref="ValidateWith"/> is present.
    /// </summary>
    /// <param name="cfg">Config instance.</param>
    /// <returns>Error message if validation fails, or null if valid.</returns>
    private static String runValidation(IDreamConfig cfg) throws Exception {
        var ann = cfg.getClass().getAnnotation(ValidateWith.class);
        if (ann == null) return null;
        @SuppressWarnings("unchecked")
        var validator = (ConfigValidator<IDreamConfig>) ann.value().getDeclaredConstructor().newInstance();
        return validator.validate(cfg);
    }
}