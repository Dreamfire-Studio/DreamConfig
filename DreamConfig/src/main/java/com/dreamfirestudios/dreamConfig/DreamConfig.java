package com.dreamfirestudios.dreamConfig;

import com.dreamfirestudios.dreamConfig.API.ConfigAPI;
import com.dreamfirestudios.dreamConfig.API.MongoAPI;
import com.dreamfirestudios.dreamConfig.Abstract.*;
import com.dreamfirestudios.dreamConfig.DreamfireVariableTest.SaveAbleInventoryKeysVariableTest;
import com.dreamfirestudios.dreamConfig.DreamfireVariableTest.SaveableLocationKeysVariableTest;
import com.dreamfirestudios.dreamConfig.DreamfireVariableTest.SavebaleItemStackKeysVariableTest;
import com.dreamfirestudios.dreamConfig.Object.MongoConnection;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import com.dreamfirestudios.dreamCore.DreamfireJava.DreamfireClassAPI;
import com.dreamfirestudios.dreamCore.DreamfireJava.DreamfireJavaAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/// TODO implement events
public class DreamConfig extends JavaPlugin {
    private static DreamConfig dreamfireConfig;
    private static final HashMap<String, MongoConnection> mongoConnectionHashMap = new HashMap<>();
    private static final HashMap<String, DynamicPulseMongo<?>> dynamicPulseMongoHashMap = new HashMap<>();
    private static final HashMap<String, StaticPulseMongo<?>> staticPulseMongoHashMap = new HashMap<>();
    private static final HashMap<String, DynamicPulseConfig<?>> dynamicPulseConfigHashMap = new HashMap<>();
    private static final HashMap<String, StaticPulseConfig<?>> staticPulseConfigHashMap = new HashMap<>();

    public static DreamConfig GetDreamfireConfig(){return dreamfireConfig;}

    @Override
    public void onEnable() {
        dreamfireConfig = this;
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.OFF);
        DreamfireClassAPI.RegisterPulseVariableTest(this, new SaveAbleInventoryKeysVariableTest());
        DreamfireClassAPI.RegisterPulseVariableTest(this, new SavebaleItemStackKeysVariableTest());
        DreamfireClassAPI.RegisterPulseVariableTest(this, new SaveableLocationKeysVariableTest());
        RegisterStatic(this, false);
    }

    public void RegisterStatic(JavaPlugin javaPlugin, boolean reset){
        try {RegisterStaticRaw(javaPlugin, reset);}
        catch (Exception e) {throw new RuntimeException(e);}
    }

    public void RegisterStaticRaw(JavaPlugin javaPlugin, boolean reset) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (var autoRegisterClass : DreamfireJavaAPI.getAutoRegisterClasses(javaPlugin)) {
            if (StaticPulseMongo.class.isAssignableFrom(autoRegisterClass)) {
                var staticPulseMongo = (StaticPulseMongo) autoRegisterClass.getConstructor().newInstance();
                if(reset){
                    MongoAPI.Delete(staticPulseMongo, iPulseMongo -> {
                        DeleteStaticPulseMongo(staticPulseMongo.documentID());
                        ReloadMongoConfig(staticPulseMongo);
                    },throwable -> {
                        throwable.printStackTrace();
                    });
                }else ReloadMongoConfig(staticPulseMongo);
            }else if (StaticPulseConfig.class.isAssignableFrom(autoRegisterClass) || StaticEnumPulseConfig.class.isAssignableFrom(autoRegisterClass)) {
                var staticPulseConfig = (StaticPulseConfig) autoRegisterClass.getConstructor().newInstance();
                if(reset){
                    ConfigAPI.Delete(staticPulseConfig, iPulseConfig -> {
                        DeleteStaticPulseConfig(staticPulseConfig.documentID());
                        ReloadConfig(staticPulseConfig);
                    },throwable -> {
                        throwable.printStackTrace();
                    });
                }else ReloadConfig(staticPulseConfig);
            }
        }
    }

    private void ReloadMongoConfig(StaticPulseMongo<?> staticPulseMongo){
        MongoAPI.Load(true, staticPulseMongo, iPulseMongo -> {
            SetStaticPulseMongo(staticPulseMongo.documentID(), staticPulseMongo);
            DreamfireChat.SendMessageToConsole(String.format("&8Reloaded StaticPulseMongo: %s", staticPulseMongo.getClass().getSimpleName()));
        }, throwable -> {
            throwable.printStackTrace();
        });
    }

    private void ReloadConfig(StaticPulseConfig<?> staticPulseConfig){
        ConfigAPI.Load(staticPulseConfig, iPulseConfig -> {
            SetStaticPulseConfig(staticPulseConfig.documentID(), staticPulseConfig);
            DreamfireChat.SendMessageToConsole(String.format("&8Reloaded StaticPulseConfig: %s", staticPulseConfig.getClass().getSimpleName()));
        }, throwable -> {
            throwable.printStackTrace();
        });
    }

    public MongoConnection GetMongoConnection(String id){return mongoConnectionHashMap.getOrDefault(id, null);}
    public void SetMongoConnection(String id, MongoConnection mongoConnection){mongoConnectionHashMap.put(id, mongoConnection);}

    public DynamicPulseMongo<?> GetDynamicPulseMongo(String id){return dynamicPulseMongoHashMap.getOrDefault(id, null);}
    public void SetDynamicPulseMongo(String id, DynamicPulseMongo<?> dynamicPulseMongo){dynamicPulseMongoHashMap.put(id, dynamicPulseMongo);}
    public void DeleteDynamicPulseMongo(String id){dynamicPulseMongoHashMap.remove(id);}
    public Collection<DynamicPulseMongo<?>> GetAllDynamicPulseMongo(){return dynamicPulseMongoHashMap.values();}

    public StaticPulseMongo<?> GetStaticPulseMongo(String id){return staticPulseMongoHashMap.getOrDefault(id, null);}
    public void SetStaticPulseMongo(String id, StaticPulseMongo<?> staticPulseMongo){staticPulseMongoHashMap.put(id, staticPulseMongo);}
    public void DeleteStaticPulseMongo(String id){staticPulseMongoHashMap.remove(id);}

    public DynamicPulseConfig<?> GetDynamicPulseConfig(String id){return dynamicPulseConfigHashMap.getOrDefault(id, null);}
    public void SetDynamicPulseConfig(String id, DynamicPulseConfig<?> dynamicPulseMongo){dynamicPulseConfigHashMap.put(id, dynamicPulseMongo);}
    public void DeleteDynamicPulseConfig(String id){dynamicPulseConfigHashMap.remove(id);}
    public Collection<DynamicPulseConfig<?>> GetAllDynamicPulseConfigs(){return dynamicPulseConfigHashMap.values();}

    public StaticPulseConfig<?> GetStaticPulseConfig(String id){return staticPulseConfigHashMap.getOrDefault(id, null);}
    public void SetStaticPulseConfig(String id, StaticPulseConfig<?> StaticPulseConfig){staticPulseConfigHashMap.put(id, StaticPulseConfig);}
    public void DeleteStaticPulseConfig(String id){staticPulseConfigHashMap.remove(id);}
}
