package com.dreamfirestudios.dreamconfig.Interface;

public interface DeserializationStrategy<T> {
    T deserialize(Object rawValue) throws Exception;
}