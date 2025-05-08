package com.dreamfirestudios.dreamConfig.Interface;

public interface DeserializationStrategy<T> {
    T deserialize(Object rawValue) throws Exception;
}
