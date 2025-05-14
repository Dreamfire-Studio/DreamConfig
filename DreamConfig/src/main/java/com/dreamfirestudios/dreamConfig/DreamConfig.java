package com.dreamfirestudios.dreamConfig;

import com.dreamfirestudios.dreamConfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamConfig.Abstract.DynamicPulseConfig;
import com.dreamfirestudios.dreamConfig.Abstract.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamConfig.Abstract.StaticPulseConfig;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import com.dreamfirestudios.dreamCore.DreamfireJava.DreamfireJavaAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class DreamConfig extends JavaPlugin {
    private static DreamConfig dreamConfig;
    public static DreamConfig GetDreamConfig(){return dreamConfig;}

    public static final HashMap<String, DynamicPulseConfig<?>> dynamicPulseConfigHashMap = new HashMap<>();
    public static final HashMap<String, StaticPulseConfig<?>> staticPulseConfigHashMap = new HashMap<>();

    @Override
    public void onEnable() {
        dreamConfig = this;
        RegisterStatic(this, false);
    }

    public void RegisterStatic(JavaPlugin javaPlugin, boolean reset){
        try {RegisterStaticRaw(javaPlugin, reset);}
        catch (Exception e) {throw new RuntimeException(e);}
    }

    public void RegisterStaticRaw(JavaPlugin javaPlugin, boolean reset) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (var autoRegisterClass : DreamfireJavaAPI.getAutoRegisterClasses(javaPlugin)) {
            if (StaticPulseConfig.class.isAssignableFrom(autoRegisterClass) || StaticEnumPulseConfig.class.isAssignableFrom(autoRegisterClass)) {
                var staticPulseConfig = (StaticPulseConfig) autoRegisterClass.getConstructor().newInstance();
                if(reset){
                    DreamConfigAPI.DeleteDreamConfig(javaPlugin, staticPulseConfig, iPulseConfig -> {
                        staticPulseConfigHashMap.remove(staticPulseConfig.documentID());
                        ReloadDreamConfig(javaPlugin, staticPulseConfig);
                    });
                }else ReloadDreamConfig(javaPlugin, staticPulseConfig);
            }
        }
    }

    private void ReloadDreamConfig(JavaPlugin javaPlugin, StaticPulseConfig<?> staticPulseConfig){
        DreamConfigAPI.LoadDreamConfig(javaPlugin, staticPulseConfig, iPulseConfig -> {
            staticPulseConfigHashMap.put(staticPulseConfig.documentID(), staticPulseConfig);
            DreamfireChat.SendMessageToConsole(String.format("&8Reloaded StaticPulseConfig: %s", staticPulseConfig.getClass().getSimpleName()));
        });
    }
}
