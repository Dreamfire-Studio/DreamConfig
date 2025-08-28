package com.dreamfirestudios.dreamconfig.Events;

import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;

/**
 * <summary>Base class for all DreamConfig config events.</summary>
 * <remarks>Subclasses must declare their own static HandlerList per Bukkit's requirements.</remarks>
 */
public abstract class AbstractConfigEvent extends Event {
    private final String configId;
    private final Instant occurredAt;
    private final JavaPlugin plugin;

    /// <summary>Create a new config event.</summary>
    /// <param name="plugin">Owning plugin</param>
    /// <param name="configId">Config document ID</param>
    public AbstractConfigEvent(JavaPlugin plugin, String configId) {
        super(true); // async-safe; we often fire from worker threads
        this.plugin = plugin;
        this.configId = configId;
        this.occurredAt = Instant.now();
    }

    /// <summary>Owning plugin.</summary>
    public JavaPlugin getPlugin() { return plugin; }

    /// <summary>Document identifier for the config.</summary>
    public String getConfigId() { return configId; }

    /// <summary>Event timestamp (UTC).</summary>
    public Instant getOccurredAt() { return occurredAt; }
}
