package com.dreamfirestudios.dreamConfig.Interface;

public interface SerializationStrategy<T> {
    T serialize(Object rawValue) throws Exception;
}
