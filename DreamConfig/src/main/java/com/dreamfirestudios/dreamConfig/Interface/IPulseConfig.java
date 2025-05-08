package com.dreamfirestudios.dreamConfig.Interface;

import com.dreamfirestudios.dreamConfig.API.ConfigAPI;
import com.dreamfirestudios.dreamConfig.Console.ConfigConsole;
import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public interface IPulseConfig {
    default JavaPlugin mainClass(){return DreamConfig.GetDreamfireConfig();}
    default String documentID(){return getClass().getSimpleName();}
    default boolean useSubFolder(){return false;}
    default void FirstLoadConfig(){}
    default void BeforeLoadConfig(){}
    default void AfterLoadConfig(){}
    default void BeforeSaveConfig(){}
    default void AfterSaveConfig(){}
    default <T extends IPulseConfig> void SaveConfig(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        @SuppressWarnings("unchecked")
        T self = (T) this; // Cast `this` to T
        ConfigAPI.Save(self, onSuccess, onError);
    }

    default <T extends IPulseConfig> void LoadConfig(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        @SuppressWarnings("unchecked")
        T self = (T) this; // Cast `this` to T
        ConfigAPI.Load(self, onSuccess, onError);
    }

    default <T extends IPulseConfig> void DeleteConfig(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        @SuppressWarnings("unchecked")
        T self = (T) this; // Cast `this` to T
        ConfigAPI.Delete(self, onSuccess, onError);
    }

    default <T extends IPulseConfig> void DisplayConfig() throws Exception {
        @SuppressWarnings("unchecked")
        T self = (T) this; // Cast `this` to T
        DreamfireChat.SendMessageToConsole(ConfigConsole.ConsoleOutput(self));
    }
}
