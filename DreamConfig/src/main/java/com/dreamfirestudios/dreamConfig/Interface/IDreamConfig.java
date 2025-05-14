package com.dreamfirestudios.dreamConfig.Interface;

import com.dreamfirestudios.dreamConfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamConfig.DreamConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public interface IDreamConfig {
    default JavaPlugin mainClass(){return DreamConfig.GetDreamConfig();}
    default String documentID(){return getClass().getSimpleName();}
    default boolean useSubFolder(){return true;}
    default void FirstLoadConfig(){}
    default void BeforeLoadConfig(){}
    default void AfterLoadConfig(){}
    default void BeforeSaveConfig(){}
    default void AfterSaveConfig(){}

    default <T extends IDreamConfig> void SaveDreamConfig(JavaPlugin javaPlugin, Consumer<T> onSuccessSave){
        DreamConfigAPI.SaveDreamConfig(javaPlugin, (T) this, onSuccessSave);
    }

    default <T extends IDreamConfig> void LoadDreamConfig(JavaPlugin javaPlugin, Consumer<T> onSuccessLoad){
        DreamConfigAPI.LoadDreamConfig(javaPlugin, (T) this, onSuccessLoad);
    }

    default <T extends IDreamConfig> void DisplayDreamConfig(Consumer<T> onSuccess) {
        DreamConfigAPI.DisplayDreamConfig((T) this, onSuccess);
    }

    default <T extends IDreamConfig> void DeleteDreamConfig(JavaPlugin javaPlugin, Consumer<T> onSuccessDelete) {
        DreamConfigAPI.DeleteDreamConfig(javaPlugin, (T) this, onSuccessDelete);
    }
}
