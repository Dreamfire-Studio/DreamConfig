package com.dreamfirestudios.dreamConfig.Abstract;

import com.dreamfirestudios.dreamConfig.API.ConfigAPI;
import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.DontSave;
import com.dreamfirestudios.dreamConfig.Interface.IPulseConfig;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class DynamicPulseConfig<T extends DynamicPulseConfig<T>> implements IPulseConfig {
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
        for(var config : DreamConfig.GetDreamfireConfig().GetAllDynamicPulseMongo()){
            if (!clazz.isInstance(config)) continue;
            data.add(clazz.cast(config));
        }
        return data;
    }

    public static <T extends DynamicPulseConfig<T>> void ReturnAllConfigsAsync(Class<T> clazz) {
        try {
            T dummyInstance = clazz.getDeclaredConstructor().newInstance();
            ConfigAPI.ReturnAllConfigDocuments(dummyInstance).thenAccept(configs -> {
                configs.forEach((configName, config) -> {
                    try {
                        T castedConfig = clazz.cast(config);
                        DreamConfig.GetDreamfireConfig().SetDynamicPulseConfig(castedConfig.documentID(), castedConfig);
                        DreamfireChat.SendMessageToConsole(String.format("&9Registered DynamicPulseConfig %s", castedConfig.documentID()));
                    } catch (ClassCastException e) {
                        DreamfireChat.SendMessageToConsole(String.format("&cFailed to cast DynamicPulseConfig %s", configName));
                        e.printStackTrace();
                    }
                });
            }).exceptionally(throwable -> {
                DreamfireChat.SendMessageToConsole("&cError loading DynamicPulseConfig: " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DynamicPulseConfig for class: " + clazz.getSimpleName(), e);
        }
    }

    public static <T extends DynamicPulseConfig<T>> void SaveConfig(T clazz, String documentID, Consumer<T> onSuccess , Consumer<Throwable> onError) {
        clazz.SaveConfig(onSuccess, onError);
        DreamConfig.GetDreamfireConfig().SetDynamicPulseConfig(documentID, clazz);
    }

    public static <T extends DynamicPulseConfig<T>> void GetConfig(Class<T> clazz, String documentID, boolean overrideExisting, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        try {
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "1");
            var storedData = DreamConfig.GetDreamfireConfig().GetDynamicPulseConfig(documentID);
            if (storedData != null && !overrideExisting) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "2");
                onSuccess.accept((T) storedData);
                return;
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "3");
            T newInstance = clazz.getDeclaredConstructor(String.class).newInstance(documentID);
            newInstance.SaveConfig(
                    loadedInstance -> {
                        try {
                            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "4");
                            DreamConfig.GetDreamfireConfig().SetDynamicPulseConfig(newInstance.documentID(), newInstance);
                            onSuccess.accept(newInstance);
                        } catch (Exception e) {
                            onError.accept(e);
                        }
                    },
                    onError
            );
        } catch (Exception e) {
            onError.accept(e);
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "5");
    }
}
