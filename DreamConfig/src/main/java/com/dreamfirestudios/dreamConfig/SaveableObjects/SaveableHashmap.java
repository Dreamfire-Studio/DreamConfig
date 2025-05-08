package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.DeSerializer.ConfigDeSerializer;
import com.dreamfirestudios.dreamConfig.DeSerializer.MongoDeSerializer;
import com.dreamfirestudios.dreamConfig.Enum.StorageType;
import com.dreamfirestudios.dreamConfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import lombok.Getter;
import org.bson.Document;

import java.util.HashMap;

public class SaveableHashmap<K, V> {
    @Getter
    private HashMap<K, V> hashMap = new HashMap<>();
    private final Class<?> keyType;
    private final Class<?> dataType;

    public SaveableHashmap(Class<?> keyType, Class<?> dataType){
        this.keyType = keyType;
        this.dataType = dataType;
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
            if(storageType == StorageType.CONFIG) deserialized = ConfigDeSerializer.ReturnClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            if (storageType == StorageType.MONGO) deserialized = MongoDeSerializer.ReturnClassFieldsMap((Document) source, pulseClassInstance.getClass(), pulseClassInstance);
            pulseClassInstance.AfterLoadConfig();
            return (K) deserialized;
        }else{
            if(storageType == StorageType.CONFIG) return (K) ConfigDeSerializer.LoadConfigSingle(targetType, source, source);
            else if (storageType == StorageType.MONGO) return (K) MongoDeSerializer.LoadMongoSingle(targetType, source, source);
        }
        throw new IllegalArgumentException("Unsupported key type for deserialization.");
    }

    private V DeSerialiseValue(StorageType storageType, Object source, Class<?> targetType) throws Exception {
        if (IPulseClass.class.isAssignableFrom(targetType)) {
            IPulseClass pulseClassInstance = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(targetType);
            pulseClassInstance.BeforeLoadConfig();
            Object deserialized = null;
            if(storageType == StorageType.CONFIG) deserialized = ConfigDeSerializer.ReturnClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            else if (storageType == StorageType.MONGO) deserialized = MongoDeSerializer.ReturnClassFieldsMap((Document) source, pulseClassInstance.getClass(), pulseClassInstance);
            pulseClassInstance.AfterLoadConfig();
            return (V) deserialized;
        }else{
            if(storageType == StorageType.CONFIG) return (V) ConfigDeSerializer.LoadConfigSingle(dataType, source, source);
            else if (storageType == StorageType.MONGO) return (V) MongoDeSerializer.LoadMongoSingle(dataType, source, source);
        }
        throw new IllegalArgumentException("Unsupported value type for deserialization.");
    }

}
