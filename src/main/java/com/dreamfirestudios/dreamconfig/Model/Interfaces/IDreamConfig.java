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
package com.dreamfirestudios.dreamconfig.Model.Interfaces;

import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

/// <summary>
/// Common hooks for all config types. Methods default to no-ops.
/// </summary>
/// <remarks>
/// Provides lifecycle hooks and convenience methods to delegate to <see cref="DreamConfigAPI"/>.
/// </remarks>
public interface IDreamConfig {
    /// <summary>Owning plugin. Defaults to <see cref="DreamConfig.get()"/>.</summary>
    default JavaPlugin mainClass() { return DreamConfig.get(); }

    /// <summary>Document id (file name or logical key).</summary>
    default String documentID() { return getClass().getSimpleName(); }

    /// <summary>When true, uses a subfolder with same name as <see cref="documentID"/> (YAML repo).</summary>
    default boolean useSubFolder() { return true; }

    /// <summary>Hook: first time a config is created.</summary>
    default void FirstLoadConfig() {}

    /// <summary>Hook: before loading.</summary>
    default void BeforeLoadConfig() {}

    /// <summary>Hook: after loading.</summary>
    default void AfterLoadConfig() {}

    /// <summary>Hook: before saving.</summary>
    default void BeforeSaveConfig() {}

    /// <summary>Hook: after saving.</summary>
    default void AfterSaveConfig() {}

    /// <summary>Save this config via <see cref="DreamConfigAPI"/>.</summary>
    @SuppressWarnings("unchecked")
    default <T extends IDreamConfig> void SaveDreamConfig(JavaPlugin plugin, Consumer<T> onSuccess) {
        DreamConfigAPI.save(plugin, (T) this, onSuccess);
    }

    /// <summary>Load this config via <see cref="DreamConfigAPI"/>.</summary>
    @SuppressWarnings("unchecked")
    default <T extends IDreamConfig> void LoadDreamConfig(JavaPlugin plugin, Consumer<T> onSuccess) {
        DreamConfigAPI.load(plugin, (T) this, onSuccess);
    }

    /// <summary>Display this config in console via <see cref="DreamConfigAPI"/>.</summary>
    @SuppressWarnings("unchecked")
    default <T extends IDreamConfig> void DisplayDreamConfig(Consumer<T> onSuccess) {
        DreamConfigAPI.display((T) this, onSuccess);
    }

    /// <summary>Delete this config via <see cref="DreamConfigAPI"/>.</summary>
    @SuppressWarnings("unchecked")
    default <T extends IDreamConfig> void DeleteDreamConfig(JavaPlugin plugin, Consumer<T> onSuccess) {
        DreamConfigAPI.delete(plugin, (T) this, onSuccess);
    }
}