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
package com.dreamfirestudios.dreamconfig.Bootstrap;

import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.Internal.Repo.MongoConfigRepository;
import com.dreamfirestudios.dreamconfig.Internal.Repo.ConfigRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bukkit.plugin.java.JavaPlugin;

/// <summary>
/// Bootstrap utility that configures the underlying <see cref="ConfigRepository"/> implementation
/// for <see cref="DreamConfigAPI"/>.
/// </summary>
/// <remarks>
/// By default, YAML storage is used.  
/// If MongoDB environment variables are provided, the repository will be switched to MongoDB.
/// This enables server admins to scale config persistence without code changes.
/// <br/>
/// Expected environment variables:
/// <list type="bullet">
/// <item><description><c>DREAMCONFIG_MONGO_URI</c> – e.g., <c>mongodb://localhost:27017</c></description></item>
/// <item><description><c>DREAMCONFIG_MONGO_DB</c> – e.g., <c>dreamconfig</c></description></item>
/// <item><description><c>DREAMCONFIG_MONGO_COLLECTION</c> – e.g., <c>configs</c></description></item>
/// </list>
/// </remarks>
/// <example>
/// <code>
/// // In plugin onEnable()
/// public void onEnable() {
///     ConfigRepositoryBootstrap.initialize(this);
///     getLogger().info("DreamConfig initialized!");
/// }
///
/// // With environment variables set:
/// // DREAMCONFIG_MONGO_URI=mongodb://127.0.0.1:27017
/// // DREAMCONFIG_MONGO_DB=dreamdb
/// // DREAMCONFIG_MONGO_COLLECTION=configs
/// </code>
/// </example>
public final class ConfigRepositoryBootstrap {

    private ConfigRepositoryBootstrap() {}

    /// <summary>
    /// Initializes the config repository based on environment variables.
    /// </summary>
    /// <param name="plugin">Owning plugin instance, used for logging and task scheduling.</param>
    /// <remarks>
    /// - If environment variables are missing, YAML is used (default).  
    /// - If environment variables are present, MongoDB is attempted.  
    /// - On MongoDB initialization failure, falls back to YAML.
    /// </remarks>
    /// <example>
    /// <code>
    /// @Override
    /// public void onEnable() {
    ///     ConfigRepositoryBootstrap.initialize(this);
    /// }
    /// </code>
    /// </example>
    public static void initialize(JavaPlugin plugin) {
        var uri  = System.getenv("DREAMCONFIG_MONGO_URI");
        var db   = System.getenv("DREAMCONFIG_MONGO_DB");
        var coll = System.getenv("DREAMCONFIG_MONGO_COLLECTION");

        if (uri == null || db == null || coll == null) {
            plugin.getLogger().info("[DreamConfig] Using YAML repository (env not present).");
            return;
        }

        try {
            MongoClient client = MongoClients.create(uri);
            ConfigRepository mongo = new MongoConfigRepository(client, db, coll);
            DreamConfigAPI.setRepository(mongo);
            plugin.getLogger().info("[DreamConfig] Mongo repository enabled: " + db + "/" + coll);
        } catch (Throwable t) {
            plugin.getLogger().severe("[DreamConfig] Failed to initialize Mongo repository. Falling back to YAML. " + t);
        }
    }
}