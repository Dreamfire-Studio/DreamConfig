package com.dreamfirestudios.dreamconfig.Interface;

import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ValidateWith {
    /**
     * <summary>Validator class that will be instantiated with a no-args constructor.</summary>
     */
    Class<? extends ConfigValidator<?>> value();
}