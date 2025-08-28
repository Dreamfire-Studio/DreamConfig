package com.dreamfirestudios.dreamconfig;

import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.Abstract.DynamicPulseConfig;
import com.dreamfirestudios.dreamconfig.Abstract.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamconfig.Abstract.StaticPulseConfig;
import com.dreamfirestudios.dreamcore.DreamChat.DreamChat;
import com.dreamfirestudios.dreamcore.DreamChat.DreamMessageSettings;
import com.dreamfirestudios.dreamcore.DreamJava.DreamfireJavaAPI;
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
        RegisterStatic(this, false, DreamMessageSettings.all());
    }

    public void RegisterStatic(JavaPlugin javaPlugin, boolean reset, DreamMessageSettings dreamMessageSettings){
        try {RegisterStaticRaw(javaPlugin, reset, dreamMessageSettings);}
        catch (Exception e) {throw new RuntimeException(e);}
    }

    public void RegisterStaticRaw(JavaPlugin javaPlugin, boolean reset, DreamMessageSettings dreamMessageSettings) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (var autoRegisterClass : DreamfireJavaAPI.getAutoRegisterClasses(javaPlugin)) {
            if (StaticPulseConfig.class.isAssignableFrom(autoRegisterClass) || StaticEnumPulseConfig.class.isAssignableFrom(autoRegisterClass)) {
                var staticPulseConfig = (StaticPulseConfig) autoRegisterClass.getConstructor().newInstance();
                if(reset){
                    DreamConfigAPI.DeleteDreamConfig(javaPlugin, staticPulseConfig, iPulseConfig -> {
                        staticPulseConfigHashMap.remove(staticPulseConfig.documentID());
                        ReloadDreamConfig(javaPlugin, staticPulseConfig, dreamMessageSettings);
                    });
                }else ReloadDreamConfig(javaPlugin, staticPulseConfig,dreamMessageSettings);
            }
        }
    }

    private void ReloadDreamConfig(JavaPlugin javaPlugin, StaticPulseConfig<?> staticPulseConfig, DreamMessageSettings dreamMessageSettings){
        DreamConfigAPI.LoadDreamConfig(javaPlugin, staticPulseConfig, iPulseConfig -> {
            staticPulseConfigHashMap.put(staticPulseConfig.documentID(), staticPulseConfig);
            DreamChat.SendMessageToConsole(String.format("&8Reloaded StaticPulseConfig: %s", staticPulseConfig.getClass().getSimpleName()), dreamMessageSettings);
        });
    }
}