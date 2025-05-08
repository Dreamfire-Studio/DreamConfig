package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.DeSerializer.ConfigDeSerializer;
import com.dreamfirestudios.dreamConfig.DeSerializer.MongoDeSerializer;
import com.dreamfirestudios.dreamConfig.Enum.StorageType;
import com.dreamfirestudios.dreamConfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import lombok.Getter;
import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Represents a saveable linked hash map that supports serialization and deserialization
 * with multiple storage types, including MongoDB and configuration files.
 *
 * @param <K> The type of keys maintained by this map.
 * @param <V> The type of mapped values.
 */
public class SaveableLinkedHashMap<K, V> {
    @Getter
    private final LinkedHashMap<K, V> hashMap = new LinkedHashMap<>();
    private final Class<?> keyType;
    private final Class<?> valueType;

    /**
     * Constructs a SaveableLinkedHashMap with specified key and value types.
     *
     * @param keyType   The type of the keys.
     * @param valueType The type of the values.
     */
    public SaveableLinkedHashMap(Class<?> keyType, Class<?> valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    /**
     * Deserializes data from a configuration map into the linked hash map.
     *
     * @param storageType The storage type being used.
     * @param configData  The configuration data to deserialize.
     * @throws Exception If deserialization fails.
     */
    public void deserializeFromConfig(StorageType storageType, HashMap<Object, Object> configData) throws Exception {
        for (var key : configData.keySet()) {
            K deserializedKey = deserializeKey(storageType, key, keyType);
            V deserializedValue = deserializeValue(storageType, configData.get(key), valueType);
            hashMap.put(deserializedKey, deserializedValue);
        }
    }

    /**
     * Deserializes data from a MongoDB document into the linked hash map.
     *
     * @param storageType The storage type being used.
     * @param document    The MongoDB document to deserialize.
     * @throws Exception If deserialization fails.
     */
    public void deserializeFromDocument(StorageType storageType, Document document) throws Exception {
        for (var key : document.keySet()) {
            K deserializedKey = deserializeKey(storageType, key, keyType);
            V deserializedValue = deserializeValue(storageType, document.get(key), valueType);
            hashMap.put(deserializedKey, deserializedValue);
        }
    }

    /**
     * Deserializes a key from the specified storage type.
     *
     * @param storageType The storage type being used.
     * @param source      The key source to deserialize.
     * @param targetType  The target type of the key.
     * @return The deserialized key.
     * @throws Exception If deserialization fails.
     */
    private K deserializeKey(StorageType storageType, Object source, Class<?> targetType) throws Exception {
        if (IPulseClass.class.isAssignableFrom(targetType)) {
            IPulseClass pulseClassInstance = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(targetType);
            pulseClassInstance.BeforeLoadConfig();
            Object deserialized = null;
            if(storageType == StorageType.CONFIG) deserialized = ConfigDeSerializer.ReturnClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            if (storageType == StorageType.MONGO) deserialized = MongoDeSerializer.ReturnClassFieldsMap((Document) source, pulseClassInstance.getClass(), pulseClassInstance);
            pulseClassInstance.AfterLoadConfig();
            return (K) deserialized;
        }else{
            if(storageType == StorageType.CONFIG) return (K) ConfigDeSerializer.LoadConfigSingle(keyType, source, source);
            else if (storageType == StorageType.MONGO) return (K) MongoDeSerializer.LoadMongoSingle(targetType, source, source);
        }
        throw new IllegalArgumentException("Unsupported key type for deserialization.");
    }

    /**
     * Deserializes a value from the specified storage type.
     *
     * @param storageType The storage type being used.
     * @param source      The value source to deserialize.
     * @param targetType  The target type of the value.
     * @return The deserialized value.
     * @throws Exception If deserialization fails.
     */
    private V deserializeValue(StorageType storageType, Object source, Class<?> targetType) throws Exception {
        if (IPulseClass.class.isAssignableFrom(targetType)) {
            IPulseClass pulseClassInstance = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(targetType);
            pulseClassInstance.BeforeLoadConfig();
            Object deserialized = null;
            if(storageType == StorageType.CONFIG) deserialized = ConfigDeSerializer.ReturnClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            else if (storageType == StorageType.MONGO) deserialized = MongoDeSerializer.ReturnClassFieldsMap((Document) source, pulseClassInstance.getClass(), pulseClassInstance);
            pulseClassInstance.AfterLoadConfig();
            return (V) deserialized;
        }else{
            if(storageType == StorageType.CONFIG) return (V) ConfigDeSerializer.LoadConfigSingle(valueType, source, source);
            else if (storageType == StorageType.MONGO) return (V) MongoDeSerializer.LoadMongoSingle(valueType, source, source);
        }
        throw new IllegalArgumentException("Unsupported value type for deserialization.");
    }
}