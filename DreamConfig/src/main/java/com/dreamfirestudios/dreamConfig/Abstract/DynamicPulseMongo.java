package com.dreamfirestudios.dreamConfig.Abstract;

import com.dreamfirestudios.dreamConfig.API.MongoAPI;
import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.DontSave;
import com.dreamfirestudios.dreamConfig.Interface.IPulseMongo;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class DynamicPulseMongo<T extends DynamicPulseMongo<T>> implements IPulseMongo {

    @DontSave
    private final String documentID;

    @Override
    public String documentID() {
        return documentID;
    }

    public DynamicPulseMongo(){
        this.documentID = UUID.randomUUID().toString();
    }
    public DynamicPulseMongo(String documentID) {
        if (documentID == null || documentID.isEmpty()) {
            throw new IllegalArgumentException("Config ID cannot be null or empty.");
        }
        this.documentID = documentID;
    }

    public static <T extends DynamicPulseMongo<T>> List<T> ReturnAllStoredConfigs(Class<T> clazz){
        var data = new ArrayList<T>();
        for(var config : DreamConfig.GetDreamfireConfig().GetAllDynamicPulseMongo()){
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + config.getClass().getSimpleName());
            if (!clazz.isInstance(config)) continue;
            data.add(clazz.cast(config));
        }
        return data;
    }

    public static <T extends DynamicPulseMongo<T>> void ReturnAllConfigsAsync(Class<T> clazz) {
        try {
            T dummyInstance = clazz.getDeclaredConstructor().newInstance();
            com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat.SendMessageToConsole(ChatColor.RED + dummyInstance.getClass().getSimpleName());
            MongoAPI.ReturnAllMongoDocumentsAsync(dummyInstance).thenAccept(configs -> {
                configs.forEach((configName, config) -> {
                    try {
                        T castedConfig = clazz.cast(config);
                        DreamConfig.GetDreamfireConfig().SetDynamicPulseMongo(castedConfig.documentID(), castedConfig);
                        DreamfireChat.SendMessageToConsole(String.format("&9Registered DynamicPulseMongo %s", castedConfig.documentID()));
                    } catch (ClassCastException e) {
                        DreamfireChat.SendMessageToConsole(String.format("&cFailed to cast DynamicPulseMongo %s", configName));
                        e.printStackTrace();
                    }
                });
            }).exceptionally(throwable -> {
                DreamfireChat.SendMessageToConsole("&cError loading DynamicPulseMongo: " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DynamicPulseMongo for class: " + clazz.getSimpleName(), e);
        }
    }

    public static <T extends DynamicPulseMongo<T>> void SaveConfig(T clazz, String documentID, boolean saveToMongo, Consumer<T> onSuccess , Consumer<Throwable> onError) {
        if(saveToMongo) clazz.SaveMongo(true, onSuccess, onError);
        DreamConfig.GetDreamfireConfig().SetDynamicPulseMongo(documentID, clazz);
    }

    public static <T extends DynamicPulseMongo<T>> void GetConfig(Class<T> clazz, String documentID, boolean overrideExisting, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        try {
            var storedData = DreamConfig.GetDreamfireConfig().GetDynamicPulseMongo(documentID);
            if (storedData != null && !overrideExisting) {
                onSuccess.accept((T) storedData);
                return;
            }
            T newInstance = clazz.getDeclaredConstructor(String.class).newInstance(documentID);
            newInstance.LoadMongo(
                    true,
                    loadedInstance -> {
                        try {
                            DreamConfig.GetDreamfireConfig().SetDynamicPulseMongo(newInstance.documentID(), newInstance);
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
    }
}
