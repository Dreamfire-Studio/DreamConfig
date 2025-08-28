package com.dreamfirestudios.dreamconfig.Abstract;

import com.dreamfirestudios.dreamconfig.API.DreamConfigAPI;
import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.Interface.IDreamConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * <summary>
 * Base for pulse configs with a static singleton per config class.
 * </summary>
 * <remarks>
 * IMPORTANT: On first access we <b>load</b> the config instead of saving a new default instance.
 * Saving a fresh instance here would clobber user changes on every startup.
 * </remarks>
 */
public abstract class StaticPulseConfig<T extends StaticPulseConfig<T>> implements IDreamConfig {
    @SuppressWarnings("unchecked")
    public static <T extends StaticPulseConfig<T>> void ReturnStaticAsync(JavaPlugin javaPlugin, Class<T> clazz, Consumer<T> onSuccess) {
        CompletableFuture.runAsync(() -> {
            try {
                T cached = (T) DreamConfig.staticPulseConfigHashMap.get(clazz.getSimpleName());
                if (cached != null) {
                    onSuccess.accept(cached);
                    return;
                }
                T instance = clazz.getDeclaredConstructor().newInstance();
                DreamConfigAPI.LoadDreamConfig(javaPlugin, instance, ignored -> {
                    DreamConfig.staticPulseConfigHashMap.put(clazz.getSimpleName(), instance);
                    onSuccess.accept(instance);
                }).join();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
