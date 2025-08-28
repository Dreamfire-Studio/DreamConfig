package com.dreamfirestudios.dreamconfig.Abstract;

import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.Interface.DontSave;
import com.dreamfirestudios.dreamconfig.Interface.IDreamConfig;
import com.dreamfirestudios.dreamcore.DreamChat.DreamChat;
import com.dreamfirestudios.dreamcore.DreamChat.DreamMessageSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class DynamicPulseConfig<T extends DynamicPulseConfig<T>> implements IDreamConfig {
    @DontSave
    private final String documentID;

    @Override
    public String documentID() {
        return documentID;
    }

    public DynamicPulseConfig() {
        this.documentID = UUID.randomUUID().toString();
    }

    public DynamicPulseConfig(String documentID) {
        if (documentID == null || documentID.isEmpty())  throw new IllegalArgumentException("Config ID cannot be null or empty.");
        this.documentID = documentID;
    }

    public static <T extends DynamicPulseConfig<T>> List<T> ReturnAllStoredConfigs(Class<T> clazz){
        var data = new ArrayList<T>();
        for(var config : DreamConfig.dynamicPulseConfigHashMap.values()){
            if (!clazz.isInstance(config)) continue;
            data.add(clazz.cast(config));
        }
        return data;
    }

    public static <T extends DynamicPulseConfig<T>> void ReturnAllConfigsAsync(JavaPlugin javaPlugin, Class<T> clazz, DreamMessageSettings dreamMessageSettings) {
        try {
            T dummyInstance = clazz.getDeclaredConstructor().newInstance();
            DreamConfigAPI.ReturnAllConfigDocuments(javaPlugin, dummyInstance).thenAccept(configs -> {
                configs.forEach((configName, config) -> {
                    try {
                        T castedConfig = clazz.cast(config);
                        DreamConfig.dynamicPulseConfigHashMap.put(castedConfig.documentID(), castedConfig);
                        DreamChat.SendMessageToConsole(String.format("&9Registered DynamicPulseConfig %s", castedConfig.documentID()), dreamMessageSettings);
                    } catch (ClassCastException e) {
                        DreamChat.SendMessageToConsole(String.format("&cFailed to cast DynamicPulseConfig %s", configName), dreamMessageSettings);
                        e.printStackTrace();
                    }
                });
            }).exceptionally(throwable -> {
                DreamChat.SendMessageToConsole("&cError loading DynamicPulseConfig: " + throwable.getMessage(), dreamMessageSettings);
                throwable.printStackTrace();
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DynamicPulseConfig for class: " + clazz.getSimpleName(), e);
        }
    }

    public static <T extends DynamicPulseConfig<T>> void SaveConfig(JavaPlugin javaPlugin, T clazz, String documentID, Consumer<T> onSuccess) {
        clazz.SaveDreamConfig(javaPlugin, onSuccess);
        DreamConfig.dynamicPulseConfigHashMap.put(documentID, clazz);
    }

    public static <T extends DynamicPulseConfig<T>> void GetConfig(JavaPlugin javaPlugin, Class<T> clazz, String documentID, boolean overrideExisting, Consumer<T> onSuccess) {
        try {
            var storedData = DreamConfig.dynamicPulseConfigHashMap.getOrDefault(documentID ,null);
            if (storedData != null && !overrideExisting) {
                onSuccess.accept((T) storedData);
                return;
            }
            T newInstance = clazz.getDeclaredConstructor(String.class).newInstance(documentID);

            newInstance.SaveDreamConfig(javaPlugin, loadedInstance -> {
                try {
                    DreamConfig.dynamicPulseConfigHashMap.put(newInstance.documentID(), newInstance);
                    onSuccess.accept(newInstance);
                } catch (Exception e) {e.printStackTrace();}
            });
        } catch (Exception e) {e.printStackTrace();}
    }
}