package com.dreamfirestudios.dreamconfig.Interface;

public interface SerializationStrategy<T> {
    T serialize(Object rawValue) throws Exception;
}