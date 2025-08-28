# DreamConfig — Developer Guide with Examples

DreamConfig is a flexible configuration core for Minecraft plugins. If you are writing your own plugin and want a robust system for configs, this guide will show you how to use it.

---

## Why Use DreamConfig?

* Simple, declarative config classes with fields.
* Automatic saving and loading to YAML (or MongoDB if configured).
* Async I/O so your main server thread is never blocked.
* Built-in validation and migrations to keep configs safe.
* Events fired when configs load, save, or migrate.
* Metrics hooks and versioning built-in for diagnostics and evolution.

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
* Use metrics timers (`MetricsTimer`) when diagnosing performance of load/save operations.

---

## Full Example Source: `DreamConfigExamples.java`

The full source code (with doc comments) is available in the repo under `com.dreamfirestudios.dreamconfig.examples`. It demonstrates static, enum, and dynamic configs, plus validation, migrations, events, metrics hooks, and console display.