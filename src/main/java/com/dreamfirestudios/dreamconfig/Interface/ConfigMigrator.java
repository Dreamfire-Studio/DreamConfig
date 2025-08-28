package com.dreamfirestudios.dreamconfig.Interface;

/**
 * <summary>A single, incremental migration step for a config type.</summary>
 * <typeparam name="T">Config class this migrator applies to.</typeparam>
 */
public interface ConfigMigrator<T> {
    /**
     * <summary>Version this migrator expects as input.</summary>
     */
    int fromVersion();

    /**
     * <summary>Version this migrator will produce.</summary>
     */
    int toVersion();

    /**
     * <summary>Apply the migration to the given config instance.</summary>
     * <param name="config">Config to mutate in-place.</param>
     */
    void migrate(T config);
}