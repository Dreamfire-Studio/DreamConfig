package com.dreamfirestudios.dreamConfig.Abstract;

import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.IDreamConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class StaticPulseConfig<T extends StaticPulseConfig<T>> implements IDreamConfig {
    public static <T extends StaticPulseConfig<T>> void ReturnStaticAsync(JavaPlugin javaPlugin, Class<T> clazz, Consumer<T> onSuccess) {
        CompletableFuture.runAsync(() -> {
            try {
                var stored = (T) DreamConfig.staticPulseConfigHashMap.getOrDefault(clazz.getSimpleName(), null);
                if (stored != null) {
                    onSuccess.accept(stored);
                    return;
                }
                T newInstance = clazz.getDeclaredConstructor().newInstance();
                newInstance.SaveDreamConfig(javaPlugin, onSuccess);
                DreamConfig.staticPulseConfigHashMap.put(clazz.getSimpleName(), newInstance);
                onSuccess.accept(newInstance);
            } catch (Exception e) {e.printStackTrace();}
        });
    }
}
