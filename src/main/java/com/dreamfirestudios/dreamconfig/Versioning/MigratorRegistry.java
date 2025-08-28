package com.dreamfirestudios.dreamconfig.Versioning;

import com.dreamfirestudios.dreamconfig.Interface.ConfigMigrator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <summary>
 * Registry and executor for config migrators. Allows registering multiple
 * incremental steps and applying them sequentially starting from a given version.
 * </summary>
 */
public final class MigratorRegistry {
    private static final Map<Class<?>, List<ConfigMigrator<?>>> REGISTRY = new ConcurrentHashMap<>();

    private MigratorRegistry() { }

    /**
     * <summary>Register a migrator for a config class.</summary>
     */
    public static <T> void register(Class<T> configClass, ConfigMigrator<T> migrator) {
        REGISTRY.computeIfAbsent(configClass, k -> new ArrayList<>()).add(migrator);
        // Keep steps ordered (by fromVersion then toVersion)
        REGISTRY.get(configClass).sort(Comparator
                .comparingInt((ConfigMigrator<?> m) -> m.fromVersion())
                .thenComparingInt(ConfigMigrator::toVersion));
    }

    /**
     * <summary>Get a copy of the registered migrators for a class, ordered.</summary>
     */
    @SuppressWarnings("unchecked")
    public static <T> List<ConfigMigrator<T>> get(Class<T> configClass) {
        var list = REGISTRY.getOrDefault(configClass, List.of());
        return (List<ConfigMigrator<T>>) (List<?>) new ArrayList<>(list);
    }

    /**
     * <summary>
     * Apply all available migrators in order, starting at <paramref name="currentVersion"/>.
     * Only runs steps whose {@code fromVersion} matches the current version as it progresses.
     * </summary>
     * <param name="config">Config to mutate in place.</param>
     * <param name="configClass">Config class key for registry lookup.</param>
     * <param name="currentVersion">Starting version (read from file metadata, for example).</param>
     * <returns>The final version after applying zero or more migrations.</returns>
     */
    @SuppressWarnings("unchecked")
    public static <T> int applyAll(T config, Class<T> configClass, int currentVersion) {
        List<ConfigMigrator<?>> steps = REGISTRY.getOrDefault(configClass, List.of());
        if (steps.isEmpty()) return currentVersion;

        int v = currentVersion;
        boolean progressed;
        do {
            progressed = false;
            for (ConfigMigrator<?> raw : steps) {
                ConfigMigrator<T> step = (ConfigMigrator<T>) raw;
                if (step.fromVersion() == v) {
                    step.migrate(config);
                    v = step.toVersion();
                    progressed = true;
                }
            }
        } while (progressed);

        return v;
    }
}