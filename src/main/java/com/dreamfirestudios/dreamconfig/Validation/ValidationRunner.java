package com.dreamfirestudios.dreamconfig.Validation;

import com.dreamfirestudios.dreamconfig.Interface.ConfigValidator;
import com.dreamfirestudios.dreamconfig.Interface.ValidateWith;

import java.lang.reflect.Constructor;

/**
 * <summary>Utility to run {@link ValidateWith} validators against a config instance.</summary>
 */
public final class ValidationRunner {
    private ValidationRunner() { }

    /**
     * <summary>Run the validator declared on the config class, if any.</summary>
     * <param name="config">Config instance.</param>
     * <returns>Error message if validation failed; otherwise {@code null}.</returns>
     */
    @SuppressWarnings("unchecked")
    public static String validateIfPresent(Object config) {
        if (config == null) return "config is null";
        Class<?> type = config.getClass();
        ValidateWith anno = type.getAnnotation(ValidateWith.class);
        if (anno == null) return null;

        try {
            Class<? extends ConfigValidator<?>> validatorType = anno.value();
            Constructor<?> ctor = validatorType.getDeclaredConstructor();
            ctor.setAccessible(true);
            ConfigValidator<Object> validator = (ConfigValidator<Object>) ctor.newInstance();
            return validator.validate(config);
        } catch (Exception e) {
            return "validation failed to run: " + e.getMessage();
        }
    }
}