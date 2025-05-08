package com.dreamfirestudios.dreamConfig.API;

import com.dreamfirestudios.dreamConfig.DeSerializer.ConfigDeSerializer;
import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.IPulseConfig;
import com.dreamfirestudios.dreamConfig.Interface.StoragePath;
import com.dreamfirestudios.dreamConfig.Object.ConfigObject;
import com.dreamfirestudios.dreamConfig.Serializer.ConfigSerializer;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import com.dreamfirestudios.dreamCore.DreamfireFile.DreamfireDir;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ConfigAPI {
    public static CompletableFuture<HashMap<String, IPulseConfig>> ReturnAllConfigDocuments(IPulseConfig iPulseConfig) {
        return CompletableFuture.supplyAsync(() -> {
            var data = new HashMap<String, IPulseConfig>();
            try {
                for (var file : DreamfireDir.returnAllFilesFromDirectory(new File(ReturnConfigPath(iPulseConfig)), false)) {
                    if (!file.getName().contains(".yml")) continue;
                    var fileName = file.getName().replace(".yml", "");
                    var newInstance = SerializerHelpers.CreateInstanceWithID(fileName, iPulseConfig.getClass());
                    if (newInstance == null) continue;
                    var pc = (IPulseConfig) newInstance;
                    Load(pc, loaded -> {}, error -> {}).join();
                    data.put(fileName, pc);
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            return data;
        });
    }

    public static String ReturnConfigPath(IPulseConfig iPulseConfig){
        if(iPulseConfig.getClass().isAnnotationPresent(StoragePath.class)) return String.format("plugins/%s", iPulseConfig.getClass().getAnnotation(StoragePath.class).value());
        if(iPulseConfig.useSubFolder()) return String.format("plugins/%s/%s", iPulseConfig.mainClass().getClass().getSimpleName(), iPulseConfig.getClass().getSimpleName());
        return String.format("plugins/%s", iPulseConfig.mainClass().getClass().getSimpleName());
    }

    public static <T extends IPulseConfig> CompletableFuture<Void> Save(T iPulseConfig, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        return CompletableFuture.runAsync(() -> {
            try {
                var configObject = new ConfigObject(ReturnConfigPath(iPulseConfig), iPulseConfig.documentID());
                if (configObject.FirstSave()) iPulseConfig.FirstLoadConfig();
                ConfigSerializer.SaveConfig(iPulseConfig, configObject);
                onSuccess.accept(iPulseConfig);
            } catch (Throwable t) {
                onError.accept(t);
            }
        });
    }

    public static <T extends IPulseConfig> CompletableFuture<Void> Load(T iPulseConfig, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        return CompletableFuture.runAsync(() -> {
            try {
                var configObject = new ConfigObject(ConfigAPI.ReturnConfigPath(iPulseConfig), iPulseConfig.documentID());
                if (configObject.FirstSave()) {
                    iPulseConfig.FirstLoadConfig();
                    Save(iPulseConfig, onSuccess, onError);
                } else {
                    ConfigDeSerializer.LoadConfig(iPulseConfig, configObject);
                    onSuccess.accept(iPulseConfig);
                }
            } catch (Throwable t) {
                onError.accept(t);
            }
        });
    }

    public static <T extends IPulseConfig> CompletableFuture<Void> Delete(T iPulseConfig, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        return CompletableFuture.runAsync(() -> {
            try {
                var configObject = new ConfigObject(ConfigAPI.ReturnConfigPath(iPulseConfig), iPulseConfig.documentID());
                configObject.DeleteConfig();
                DreamConfig.GetDreamfireConfig().DeleteDynamicPulseConfig(iPulseConfig.documentID());
                DreamConfig.GetDreamfireConfig().DeleteStaticPulseConfig(iPulseConfig.getClass().getSimpleName());
                onSuccess.accept(iPulseConfig);
            } catch (Throwable t) {
                onError.accept(t);
            }
        });
    }

}
