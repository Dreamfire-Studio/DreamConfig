package com.dreamfirestudios.dreamconfig.Events;

import com.dreamfirestudios.dreamconfig.Events.AbstractConfigEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/** <summary>Fired when a config has been successfully loaded (including first load).</summary> */
public final class ConfigLoadedEvent extends AbstractConfigEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ConfigLoadedEvent(JavaPlugin plugin, String configId) {
        super(plugin, configId);
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}