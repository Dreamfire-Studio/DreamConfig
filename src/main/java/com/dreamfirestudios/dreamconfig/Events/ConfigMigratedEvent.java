package com.dreamfirestudios.dreamconfig.Events;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * <summary>Fired after a config migration completes.</summary>
 * <remarks>Carries version info to keep your version control story intact.</remarks>
 */
public final class ConfigMigratedEvent extends AbstractConfigEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String fromVersion;
    private final String toVersion;

    /// <summary>Create a migration event.</summary>
    public ConfigMigratedEvent(JavaPlugin plugin, String configId, String fromVersion, String toVersion) {
        super(plugin, configId);
        this.fromVersion = fromVersion;
        this.toVersion   = toVersion;
    }

    /// <summary>Version before migration.</summary>
    public String getFromVersion() { return fromVersion; }

    /// <summary>Version after migration.</summary>
    public String getToVersion() { return toVersion; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}