package com.dreamfirestudios.dreamConfig.Interface;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CustomDeserialize {
    Class<? extends DeserializationStrategy<?>> strategy();
}