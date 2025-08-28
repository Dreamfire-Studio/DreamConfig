package com.dreamfirestudios.dreamconfig.Events;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/** <summary>Fired when a config file has been removed.</summary> */
public final class ConfigDeletedEvent extends AbstractConfigEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ConfigDeletedEvent(JavaPlugin plugin, String configId) {
        super(plugin, configId);
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}