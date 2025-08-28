package com.dreamfirestudios.dreamconfig.Events;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/** <summary>Fired after a config has been persisted to disk.</summary> */
public final class ConfigSavedEvent extends AbstractConfigEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ConfigSavedEvent(JavaPlugin plugin, String configId) {
        super(plugin, configId);
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}