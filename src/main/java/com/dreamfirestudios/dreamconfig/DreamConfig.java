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
package com.dreamfirestudios.dreamconfig;

import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.Bootstrap.ConfigRepositoryBootstrap;
import com.dreamfirestudios.dreamconfig.Internal.ConfigExecutor;
import com.dreamfirestudios.dreamconfig.Internal.ConfigLog;
import com.dreamfirestudios.dreamconfig.Model.DynamicPulseConfig;
import com.dreamfirestudios.dreamconfig.Model.StaticPulseConfig;
import com.dreamfirestudios.dreamcore.DreamChat.DreamChat;
import com.dreamfirestudios.dreamcore.DreamChat.DreamMessageSettings;
import com.dreamfirestudios.dreamcore.DreamJava.DreamfireJavaAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// <summary>
/// Plugin entrypoint for the DreamConfig system. Manages static/dynamic config caches and bootstraps auto‑registration.
/// </summary>
/// <remarks>
/// On enable: initializes logging, executors, repository bootstrap, and registers discovered static configs.  
/// On disable: gracefully shuts down the executor. Caches are exposed via <see cref="DYNAMIC_CACHE"/> and <see cref="STATIC_CACHE"/>.
/// </remarks>
/// <example>
/// <code>
/// public final class MyPlugin extends JavaPlugin{
///   public void onEnable(){ getServer().getPluginManager().registerEvents(..., this); }
/// }
/// </code>
/// </example>
public final class DreamConfig extends JavaPlugin {
    private static DreamConfig instance;

    /// <summary>Global access to the plugin instance.</summary>
    public static DreamConfig get() { return instance; }

    /// <summary>In‑memory cache for dynamic configs keyed by document id.</summary>
    public static final Map<String, DynamicPulseConfig<?>> DYNAMIC_CACHE = new ConcurrentHashMap<>();

    /// <summary>In‑memory cache for static configs keyed by class simple name (or custom doc id).</summary>
    public static final Map<String, StaticPulseConfig<?>> STATIC_CACHE = new ConcurrentHashMap<>();

    /// <summary>Plugin startup hook.</summary>
    @Override public void onEnable() {
        instance = this;
        ConfigLog.init(getLogger());
        ConfigExecutor.init();
        ConfigRepositoryBootstrap.initialize(this);
        registerStatic(this, false);
    }

    /// <summary>Plugin shutdown hook (graceful).</summary>
    @Override public void onDisable() { ConfigExecutor.shutdown(); }

    /// <summary>
    /// Register all <see cref="StaticPulseConfig"/> subclasses discovered by <see cref="DreamfireJavaAPI"/>.
    /// </summary>
    /// <param name="plugin">Host plugin.</param>
    /// <param name="reset">If true, delete and recreate stored configs for these classes.</param>
    /// <example>
    /// <code>
    /// getServer().getScheduler().runTask(this, () -> DreamConfig.get().registerStatic(this, false));
    /// </code>
    /// </example>
    public void registerStatic(JavaPlugin plugin, boolean reset) {
        try { registerStaticRaw(plugin, reset); }
        catch (Exception e) { throw new RuntimeException("Failed to register static configs", e); }
    }

    /// <summary>Internal registration routine.</summary>
    private void registerStaticRaw(JavaPlugin plugin, boolean reset)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (var autoClass : DreamfireJavaAPI.getAutoRegisterClasses(plugin)) {
            if (StaticPulseConfig.class.isAssignableFrom(autoClass)) {
                var staticCfg = (StaticPulseConfig<?>) autoClass.getConstructor().newInstance();
                if (reset) {
                    DreamConfigAPI.delete(plugin, staticCfg, ignored -> {
                        STATIC_CACHE.remove(staticCfg.documentID());
                        reloadStatic(plugin, staticCfg);
                    });
                } else reloadStatic(plugin, staticCfg);
            }
        }
    }

    /// <summary>Load and cache a single static config.</summary>
    private void reloadStatic(JavaPlugin plugin, StaticPulseConfig<?> cfg) {
        DreamConfigAPI.load(plugin, cfg, loaded -> {
            STATIC_CACHE.put(cfg.documentID(), cfg);
            DreamChat.SendMessageToConsole(
                    String.format("&8Reloaded StaticPulseConfig: %s", cfg.getClass().getSimpleName()),
                    DreamMessageSettings.all()
            );
        });
    }
}