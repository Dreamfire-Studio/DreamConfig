package com.dreamfirestudios.dreamconfig.Events;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/** <summary>Fired when a config fails validation.</summary> */
public final class ConfigValidationFailedEvent extends AbstractConfigEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String reason;

    public ConfigValidationFailedEvent(JavaPlugin plugin, String configId, String reason) {
        super(plugin, configId);
        this.reason = reason;
    }

    public String getReason() { return reason; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}