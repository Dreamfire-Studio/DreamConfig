package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.DeSerializer.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamConfig.Enum.StorageType;
import com.dreamfirestudios.dreamConfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import lombok.Getter;
import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Function;

public class SaveableLinkedHashMap<K, V> {
    @Getter
    private final LinkedHashMap<K, V> hashMap = new LinkedHashMap<>();
    private final Class<?> keyType;
    private final Class<?> dataType;

    public SaveableLinkedHashMap(Class<?> keyType, Class<?> valueType) {
        this.keyType = keyType;
        this.dataType = valueType;
    }

    public LinkedHashMap<Object, Object> Serialize(Function<Object, Object> saveConfigSingle){
        var result  = new LinkedHashMap<Object, Object>();
        for(var key : hashMap.keySet()) result.put(saveConfigSingle.apply(key), saveConfigSingle.apply(hashMap.get(key)));
        return result ;
    }

    public void DeSerialiseData(StorageType saveableType, HashMap<Object, Object> configData) throws Exception {
        for(var key : configData.keySet()){
            var dx = DeSerialiseKey(saveableType, key, keyType);
            var dy = DeSerialiseValue(saveableType, configData.get(key), dataType);
            hashMap.put(dx, dy);
        }
    }

    public void DeSerialiseData(StorageType saveableType, Document document) throws Exception {
        for(var key : document.keySet()){
            var dx = DeSerialiseKey(saveableType, key, keyType);
            var dy = DeSerialiseValue(saveableType, document.get(key), dataType);
            hashMap.put(dx, dy);
        }
    }

    private K DeSerialiseKey(StorageType storageType, Object source, Class<?> targetType) throws Exception {
        if (IPulseClass.class.isAssignableFrom(targetType)) {
            IPulseClass pulseClassInstance = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(targetType);
            pulseClassInstance.BeforeLoadConfig();
            Object deserialized = null;
            if(storageType == StorageType.CONFIG) deserialized = DreamConfigDeSerializer.ReturnAllClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            pulseClassInstance.AfterLoadConfig();
            return (K) deserialized;
        }else{
            if(storageType == StorageType.CONFIG) return (K) DreamConfigDeSerializer.LoadConfigSingle(targetType, source, source);
        }
        throw new IllegalArgumentException("Unsupported key type for deserialization.");
    }

    private V DeSerialiseValue(StorageType storageType, Object source, Class<?> targetType) throws Exception {
        if (IPulseClass.class.isAssignableFrom(targetType)) {
            IPulseClass pulseClassInstance = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(targetType);
            pulseClassInstance.BeforeLoadConfig();
            Object deserialized = null;
            if(storageType == StorageType.CONFIG) deserialized = DreamConfigDeSerializer.ReturnAllClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            pulseClassInstance.AfterLoadConfig();
            return (V) deserialized;
        }else{
            if(storageType == StorageType.CONFIG) return (V) DreamConfigDeSerializer.LoadConfigSingle(dataType, source, source);
        }
        throw new IllegalArgumentException("Unsupported value type for deserialization.");
    }
}
