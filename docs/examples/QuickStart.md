# DreamConfig — Developer Guide with Examples

DreamConfig is a flexible configuration core for Minecraft plugins. If you are writing your own plugin and want a robust system for configs, this guide will show you how to use it.

---

## Why Use DreamConfig?

* Simple, declarative config classes with fields.
* Automatic saving and loading to YAML (or MongoDB if configured).
* Async I/O so your main server thread is never blocked.
* Built-in validation and migrations to keep configs safe.
* Events fired when configs load, save, or migrate.

---

## Static Config Example — GameRules

```java
@ValidateWith(GameRulesValidator.class)
@ConfigVersion(3)
public static final class GameRules extends StaticPulseConfig<GameRules> {
    public boolean pvpEnabled = true;
    public int maxPlayers = 20;
    public final Limits limits = new Limits();
    public final SaveableArrayList<Material> bannedBlocks = new SaveableArrayList<>(Material.class);

    public static final class Limits implements IPulseClass {
        public int maxHomes = 3;
        public int maxClaimSize = 64;
        @Override public void BeforeSaveConfig() { /* clamp or normalize values */ }
    }
}
```

**Validator:**

```java
public static final class GameRulesValidator implements ConfigValidator<GameRules> {
    @Override
    public String validate(GameRules cfg) {
        if (cfg.maxPlayers < 1 || cfg.maxPlayers > 200) return "maxPlayers must be between 1 and 200";
        if (cfg.limits.maxHomes < 0) return "limits.maxHomes cannot be negative";
        if (cfg.limits.maxClaimSize < 16 || cfg.limits.maxClaimSize > 512) return "limits.maxClaimSize must be between 16 and 512";
        return null;
    }
}
```

**Migrations:**

```java
public static final class GameRulesV1toV2 implements ConfigMigrator<GameRules> {
    @Override public int fromVersion() { return 1; }
    @Override public int toVersion()   { return 2; }
    @Override public void migrate(GameRules cfg) {
        if (cfg.limits.maxClaimSize < 32) cfg.limits.maxClaimSize = 32;
        if (cfg.maxPlayers > 150) cfg.maxPlayers = 150;
    }
}

public static final class GameRulesV2toV3 implements ConfigMigrator<GameRules> {
    @Override public int fromVersion() { return 2; }
    @Override public int toVersion()   { return 3; }
    @Override public void migrate(GameRules cfg) {
        cfg.pvpEnabled = cfg.pvpEnabled; // no-op placeholder
    }
}
```

**Usage:**

```java
StaticPulseConfig.getAsync(plugin, GameRules.class, cfg -> {
    cfg.DisplayDreamConfig(ignored -> {});
});
```

---

## Static Enum Config Example — LootChances

```java
public enum LootKey { DIRT, IRON, GOLD, DIAMOND }

@ConfigVersion(1)
public static final class LootChances extends StaticEnumPulseConfig<LootChances, LootKey, Double> {
    @Override protected Class<LootKey> getKeyClass() { return LootKey.class; }
    @Override protected Class<Double> getValueClass() { return Double.class; }
    @Override protected Double getDefaultValueFor(LootKey key) {
        return switch (key) {
            case DIRT -> 1.0D;
            case IRON -> 0.25D;
            case GOLD -> 0.10D;
            case DIAMOND -> 0.02D;
        };
    }
}
```

**Usage:**

```java
StaticPulseConfig.getAsync(plugin, LootChances.class, loot -> {
    loot.values.getHashMap().put(LootKey.DIAMOND, 0.05D);
    loot.SaveDreamConfig(plugin, saved -> {});
});
```

---

## Dynamic Config Example — PlayerProfile

```java
@ConfigVersion(1)
public static class PlayerProfile extends DynamicPulseConfig<PlayerProfile> {
    public String nickname = "Player";
    public final SaveableArrayList<Material> favourites = new SaveableArrayList<>(Material.class);
    public final SaveableLinkedHashMap<String, String> preferences =
        new SaveableLinkedHashMap<>(String.class, String.class);

    public PlayerProfile() { super(); }
    public PlayerProfile(@SaveName("id") String id) { super(id); }
}
```

**Usage:**

```java
DynamicPulseConfig.getById(plugin, PlayerProfile.class, "player-123", false, profile -> {
    profile.nickname = "Alice";
    profile.favourites.getArrayList().add(Material.OAK_PLANKS);
    profile.SaveDreamConfig(plugin, saved -> saved.DisplayDreamConfig(ignored -> {}));
});

DynamicPulseConfig.loadAllAsync(plugin, PlayerProfile.class);
```

---

## Events Example

DreamConfig fires events when configs are loaded, saved, deleted, validated, or migrated. All events are dispatched on the **main thread**.

```java
public static final class ExampleListener implements Listener {
    @EventHandler
    public void onLoaded(ConfigLoadedEvent e) {
        Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Loaded: " + e.getConfig().documentID());
    }

    @EventHandler
    public void onSaved(ConfigSavedEvent e) {
        Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Saved: " + e.getConfig().documentID());
    }

    @EventHandler
    public void onDeleted(ConfigDeletedEvent e) {
        Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Deleted: " + e.getConfig().documentID());
    }

    @EventHandler
    public void onValidationFailed(ConfigValidationFailedEvent e) {
        Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Validation failed for " +
            e.getConfig().documentID() + " -> " + e.getMessage());
    }

    @EventHandler
    public void onMigrated(ConfigMigratedEvent e) {
        Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Migrated " +
            e.getConfig().documentID() + " " + e.getFromVersion() + " -> " + e.getToVersion());
    }
}
```

---

## Putting It Together

```java
public static void registerAll(JavaPlugin plugin) {
    // Register migrators
    MigratorRegistry.register(GameRules.class, new GameRulesV1toV2());
    MigratorRegistry.register(GameRules.class, new GameRulesV2toV3());

    // Register listener
    Bukkit.getPluginManager().registerEvents(new ExampleListener(), plugin);

    // Static config
    StaticPulseConfig.getAsync(plugin, GameRules.class, cfg -> cfg.DisplayDreamConfig(ignored -> {}));

    // Enum config
    StaticPulseConfig.getAsync(plugin, LootChances.class, loot -> {
        loot.values.getHashMap().put(LootKey.DIAMOND, 0.05D);
        loot.SaveDreamConfig(plugin, saved -> {});
    });

    // Dynamic config
    DynamicPulseConfig.getById(plugin, PlayerProfile.class, "player-" + UUID.randomUUID(), false, profile -> {
        profile.nickname = "Newbie";
        profile.SaveDreamConfig(plugin, saved -> {});
    });

    DynamicPulseConfig.loadAllAsync(plugin, PlayerProfile.class);

    DynamicPulseConfig.getById(plugin, PlayerProfile.class, "example-player", true, profile -> {
        profile.nickname = "Example";
        profile.favourites.getArrayList().add(Material.OAK_PLANKS);
        profile.SaveDreamConfig(plugin, saved -> saved.DisplayDreamConfig(ignored -> {}));
    });
}
```

---

## Tips

* Everything runs async, so avoid direct Bukkit API calls inside config callbacks.
* Use `@ValidateWith` to enforce limits.
* Use migrations to safely update old configs.
* All configs store their version under `__meta.version`.

---

## Full Example Source: `DreamConfigExamples.java`

```java
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
package com.dreamfirestudios.dreamconfig.examples;

import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.events.*;
import com.dreamfirestudios.dreamconfig.Model.DynamicPulseConfig;
import com.dreamfirestudios.dreamconfig.Model.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamconfig.Model.StaticPulseConfig;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IPulseClass;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.SaveName;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableArrayList;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamconfig.Validation.ConfigValidator;
import com.dreamfirestudios.dreamconfig.Validation.ValidateWith;
import com.dreamfirestudios.dreamconfig.versioning.ConfigMigrator;
import com.dreamfirestudios.dreamconfig.versioning.ConfigVersion;
import com.dreamfirestudios.dreamconfig.versioning.MigratorRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
/// <summary>
/// Turn‑key examples for DreamConfig v2:
///  • Static config with validation & migrations
///  • Static enum‑keyed config
///  • Dynamic (multi‑doc) config
///  • Event subscription
///  • Bulk load and console display
///  • Nested pulse classes + saveable collections
/// </summary>
/// <remarks>
/// Call {@link #registerAll(JavaPlugin)} from your plugin's {@code onEnable()} to wire everything.
/// </remarks>
/// <example>
/// <code>
/// @Override
/// public void onEnable() {
///     DreamConfigExamples.registerAll(this);
/// }
/// </code>
/// </example>
*/
public final class DreamConfigExamples {

    private DreamConfigExamples() {}

    /**
    /// <summary>
    /// Registers migrators, listeners, and kicks off example loads/saves — all async, with Bukkit events
    /// dispatched on the main thread per the platform’s requirement.
    /// </summary>
    /// <param name="plugin">Owning plugin.</param>
    public static void registerAll(JavaPlugin plugin) {
        // 1) Register example migrators (run automatically during loads when versions are behind).
        MigratorRegistry.register(GameRules.class, new GameRulesV1toV2());
        MigratorRegistry.register(GameRules.class, new GameRulesV2toV3());

        // 2) Register example listener for config lifecycle events.
        Bukkit.getPluginManager().registerEvents(new ExampleListener(), plugin);

        // 3) Load or create the static config (versioned + validated).
        StaticPulseConfig.getAsync(plugin, GameRules.class, cfg -> {
            // Show the config tree in console (purely diagnostic).
            cfg.DisplayDreamConfig(ignored -> {});
        });

        // 4) Load the enum‑keyed static config (auto‑default for all keys).
        StaticPulseConfig.getAsync(plugin, LootChances.class, loot -> {
            // Example update then save.
            loot.values.getHashMap().put(LootKey.DIAMOND, 0.05D);
            loot.SaveDreamConfig(plugin, saved -> {});
        });

        // 5) Get or create a dynamic config by ID; if absent, a new instance is created and saved.
        DynamicPulseConfig.getById(plugin, PlayerProfile.class, "player-" + UUID.randomUUID(), false, profile -> {
            profile.nickname = "Newbie";
            profile.SaveDreamConfig(plugin, saved -> {});
        });

        // 6) Bulk load all dynamic PlayerProfile docs from disk and cache them.
        DynamicPulseConfig.loadAllAsync(plugin, PlayerProfile.class);

        // 7) Display a dynamic config tree after creating a deterministic one.
        DynamicPulseConfig.getById(plugin, PlayerProfile.class, "example-player", true, profile -> {
            profile.nickname = "Example";
            profile.favourites.getArrayList().add(Material.OAK_PLANKS);
            profile.SaveDreamConfig(plugin, saved -> saved.DisplayDreamConfig(ignored -> {}));
        });
    }

    /* ============================================================
     *                       STATIC CONFIG
     * ============================================================ */

    /**
    /// <summary>
    /// Example of a simple static config with validation and migrations.
    /// Stored under a single document (default: class name) and auto‑loaded on boot.
    /// </summary>
    */
    @ValidateWith(GameRulesValidator.class)
    @ConfigVersion(3)
    public static final class GameRules extends StaticPulseConfig<GameRules> {
        /// <summary>Whether PvP is allowed.</summary>
        public boolean pvpEnabled = true;

        /// <summary>Max players allowed by the game rules (not server slots).</summary>
        public int maxPlayers = 20;

        /// <summary>Nested “pulse” class for world limits.</summary>
        public final Limits limits = new Limits();

        /// <summary>Showcase of a saveable array list with Bukkit types.</summary>
        public final SaveableArrayList<Material> bannedBlocks = new SaveableArrayList<>(Material.class);

        /// <summary>Example nested pulse class with its own lifecycle hooks.</summary>
        public static final class Limits implements IPulseClass {
            /// <summary>Max homes per player.</summary>
            public int maxHomes = 3;

            /// <summary>Max claim size in chunks.</summary>
            public int maxClaimSize = 64;

            @Override public void BeforeSaveConfig() { /* could clamp or normalize values */ }
        }
    }

    /// <summary>Validator enforcing simple guardrails.</summary>
    public static final class GameRulesValidator implements ConfigValidator<GameRules> {
        /// <inheritdoc />
        @Override
        public String validate(GameRules cfg) {
            if (cfg.maxPlayers < 1 || cfg.maxPlayers > 200) return "maxPlayers must be between 1 and 200";
            if (cfg.limits.maxHomes < 0) return "limits.maxHomes cannot be negative";
            if (cfg.limits.maxClaimSize < 16 || cfg.limits.maxClaimSize > 512) return "limits.maxClaimSize must be between 16 and 512";
            return null; // valid
        }
    }

    /// <summary>Migration: v1 -> v2 (raise minimum claim size & cap max players).</summary>
    public static final class GameRulesV1toV2 implements ConfigMigrator<GameRules> {
        @Override public int fromVersion() { return 1; }
        @Override public int toVersion()   { return 2; }
        @Override public void migrate(GameRules cfg) {
            if (cfg.limits.maxClaimSize < 32) cfg.limits.maxClaimSize = 32;
            if (cfg.maxPlayers > 150) cfg.maxPlayers = 150;
        }
    }

    /// <summary>Migration: v2 -> v3 (PVP default flip for demonstrations).</summary>
    public static final class GameRulesV2toV3 implements ConfigMigrator<GameRules> {
        @Override public int fromVersion() { return 2; }
        @Override public int toVersion()   { return 3; }
        @Override public void migrate(GameRules cfg) {
            // Demonstration change only
            cfg.pvpEnabled = cfg.pvpEnabled; // no-op, placeholder to show structure
        }
    }

    /* ============================================================
     *                 STATIC ENUM‑KEYED CONFIG
     * ============================================================ */

    /// <summary>Enum keys for the loot chances config.</summary>
    public enum LootKey { DIRT, IRON, GOLD, DIAMOND }

    /**
    /// <summary>
    /// Static config backed by enum keys. All keys are ensured to exist with defaults.
    /// </summary>
    */
    @ConfigVersion(1)
    public static final class LootChances extends StaticEnumPulseConfig<LootChances, LootKey, Double> {
        @Override protected Class<LootKey> getKeyClass() { return LootKey.class; }
        @Override protected Class<Double> getValueClass() { return Double.class; }
        @Override protected Double getDefaultValueFor(LootKey key) {
            return switch (key) {
                case DIRT -> 1.0D;
                case IRON -> 0.25D;
                case GOLD -> 0.10D;
                case DIAMOND -> 0.02D;
            };
        }
    }

    /* ============================================================
     *                      DYNAMIC CONFIG
     * ============================================================ */

    /**
    /// <summary>
    /// Example of a dynamic config (many documents). Each profile is stored under its own document ID.
    /// </summary>
    */
    @ConfigVersion(1)
    public static class PlayerProfile extends DynamicPulseConfig<PlayerProfile> {
        /// <summary>Visible nickname.</summary>
        public String nickname = "Player";

        /// <summary>Favourite blocks list.</summary>
        public final SaveableArrayList<Material> favourites = new SaveableArrayList<>(Material.class);

        /// <summary>Arbitrary preferences keyed by string.</summary>
        public final SaveableLinkedHashMap<String, String> preferences =
                new SaveableLinkedHashMap<>(String.class, String.class);

        /// <summary>Construct with random id.</summary>
        public PlayerProfile() { super(); }

        /// <summary>Construct with explicit id.</summary>
        public PlayerProfile(@SaveName("id") String id) { super(id); }
    }

    /* ============================================================
     *                       EVENT LISTENER
     * ============================================================ */

    /**
    /// <summary>
    /// Example listener that reacts to DreamConfig events. These are dispatched on the main thread.
    /// </summary>
    */
    public static final class ExampleListener implements Listener {

        /// <summary>Called when any config finishes loading (after migrations & validation).</summary>
        @EventHandler
        public void onLoaded(ConfigLoadedEvent e) {
            Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Loaded: " + e.getConfig().documentID());
        }

        /// <summary>Called after a config is saved.</summary>
        @EventHandler
        public void onSaved(ConfigSavedEvent e) {
            Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Saved: " + e.getConfig().documentID());
        }

        /// <summary>Called after a config is deleted.</summary>
        @EventHandler
        public void onDeleted(ConfigDeletedEvent e) {
            Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Deleted: " + e.getConfig().documentID());
        }

        /// <summary>Called when validation fails for a config.</summary>
        @EventHandler
        public void onValidationFailed(ConfigValidationFailedEvent e) {
            Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Validation failed for " +
                    e.getConfig().documentID() + " -> " + e.getMessage());
        }

        /// <summary>Called when a config is migrated between versions.</summary>
        @EventHandler
        public void onMigrated(ConfigMigratedEvent e) {
            Bukkit.getConsoleSender().sendMessage("[DreamConfigExamples] Migrated " +
                    e.getConfig().documentID() + " " + e.getFromVersion() + " -> " + e.getToVersion());
        }
    }
}
```

> The code above is a complete, practical reference you can copy into your project. It demonstrates static, enum, and dynamic configs, plus validation, migrations, events, and console display.