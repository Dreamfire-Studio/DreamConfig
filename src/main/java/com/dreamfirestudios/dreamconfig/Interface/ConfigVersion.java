package com.dreamfirestudios.dreamconfig.Interface;

import java.lang.annotation.*;

/**
 * <summary>Declares the schema/data version of a config class.</summary>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigVersion {
    /**
     * <summary>Monotonic integer version for the config schema.</summary>
     */
    int value();
}
