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
package com.dreamfirestudios.dreamconfig.Model;

import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.DontSave;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamcore.DreamChat.DreamChat;
import com.dreamfirestudios.dreamcore.DreamChat.DreamMessageSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

/// <summary>
/// Dynamic configs identified by a user-supplied or random document id.
/// Multiple instances per class are supported.
/// </summary>
/// <typeparam name="T">Self type.</typeparam>
/// <remarks>
/// Unlike static configs, many documents of the same class may coexist.
/// IDs must be unique across instances.
/// </remarks>
public abstract class DynamicPulseConfig<T extends DynamicPulseConfig<T>> implements IDreamConfig {
    @DontSave private final String documentID;
    @Override public final String documentID() { return documentID; }

    /// <summary>Constructs a new config with a random UUID document id.</summary>
    protected DynamicPulseConfig() { this(UUID.randomUUID().toString()); }

    /// <summary>Constructs a new config with an explicit document id.</summary>
    /// <param name="id">Unique document id.</param>
    protected DynamicPulseConfig(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Config ID cannot be null or empty.");
        this.documentID = id;
    }

    /// <summary>Returns all cached configs of the given type (no disk access).</summary>
    /// <typeparam name="T">Config subclass type.</typeparam>
    /// <returns>List of cached configs.</returns>
    @SuppressWarnings("unchecked")
    public static <T extends DynamicPulseConfig<T>> List<T> getAllCached(Class<T> clazz) {
        var list = new ArrayList<T>();
        for (var cfg : DreamConfig.DYNAMIC_CACHE.values()) if (clazz.isInstance(cfg)) list.add((T) cfg);
        return list;
    }

    /// <summary>Loads all stored documents of this class and caches them asynchronously.</summary>
    /// <param name="plugin">Owning plugin.</param>
    /// <param name="clazz">Config subclass type.</param>
    public static <T extends DynamicPulseConfig<T>> void loadAllAsync(JavaPlugin plugin, Class<T> clazz) {
        try {
            T probe = clazz.getDeclaredConstructor().newInstance();
            DreamConfigAPI.loadAll(plugin, probe).thenAccept(map -> map.forEach((name, cfg) -> {
                try {
                    T typed = clazz.cast(cfg);
                    DreamConfig.DYNAMIC_CACHE.put(typed.documentID(), typed);
                    DreamChat.SendMessageToConsole("&9Registered DynamicPulseConfig " + typed.documentID(), DreamMessageSettings.all());
                } catch (ClassCastException e) {
                    DreamChat.SendMessageToConsole("&cFailed to cast DynamicPulseConfig " + name, DreamMessageSettings.all());
                }
            }));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DynamicPulseConfig for " + clazz.getSimpleName(), e);
        }
    }

    /// <summary>Save this config asynchronously and refresh in cache.</summary>
    /// <param name="plugin">Owning plugin.</param>
    /// <param name="instance">Config instance to save.</param>
    /// <param name="onSuccess">Callback when saved.</param>
    @SuppressWarnings("unchecked")
    public static <T extends DynamicPulseConfig<T>> void save(JavaPlugin plugin, T instance, Consumer<T> onSuccess) {
        DreamConfigAPI.save(plugin, instance, saved -> {
            DreamConfig.DYNAMIC_CACHE.put(instance.documentID(), instance);
            onSuccess.accept((T) saved);
        });
    }

    /// <summary>
    /// Get (or create) a document by id.
    /// If <paramref name="overrideExisting"/> is false, returns cached instance if present.
    /// </summary>
    /// <param name="plugin">Owning plugin.</param>
    /// <param name="clazz">Config class.</param>
    /// <param name="id">Document id.</param>
    /// <param name="overrideExisting">If false, reuse cached config if available.</param>
    /// <param name="onSuccess">Callback with loaded instance.</param>
    @SuppressWarnings("unchecked")
    public static <T extends DynamicPulseConfig<T>> void getById(JavaPlugin plugin, Class<T> clazz, String id, boolean overrideExisting, Consumer<T> onSuccess) {
        try {
            if (!overrideExisting) {
                var cached = DreamConfig.DYNAMIC_CACHE.get(id);
                if (cached != null && clazz.isInstance(cached)) {
                    onSuccess.accept((T) cached);
                    return;
                }
            }
            T inst = clazz.getDeclaredConstructor(String.class).newInstance(id);
            DreamConfigAPI.load(plugin, inst, ignored -> {
                DreamConfig.DYNAMIC_CACHE.put(inst.documentID(), inst);
                onSuccess.accept(inst);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to get dynamic config " + id, e);
        }
    }
}