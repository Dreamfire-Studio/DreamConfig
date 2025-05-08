package com.dreamfirestudios.dreamConfig.Abstract;

import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.IPulseConfig;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class StaticPulseConfig<T extends StaticPulseConfig<T>> implements IPulseConfig {
    public static <T extends StaticPulseConfig<T>> void ReturnStaticAsync(Class<T> clazz, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                var stored = (T) DreamConfig.GetDreamfireConfig().GetStaticPulseConfig(clazz.getSimpleName());
                if (stored != null) {
                    onSuccess.accept(stored);
                    return;
                }
                T newInstance = clazz.getDeclaredConstructor().newInstance();
                newInstance.SaveConfig(onSuccess, onError); // Save the new instance
                DreamConfig.GetDreamfireConfig().SetStaticPulseConfig(clazz.getSimpleName(), newInstance);
                onSuccess.accept(newInstance); // Call success callback with the new instance
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }
}
