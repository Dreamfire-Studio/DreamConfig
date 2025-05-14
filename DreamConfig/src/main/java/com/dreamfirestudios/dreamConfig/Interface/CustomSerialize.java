package com.dreamfirestudios.dreamConfig.Interface;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CustomSerialize {
    Class<? extends SerializationStrategy<?>> serializationStrategy();
    Class<? extends DeserializationStrategy<?>> deserializationStrategy();
}
