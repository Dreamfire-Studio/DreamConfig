package com.dreamfirestudios.dreamconfig.SaveableObjects;

import com.dreamfirestudios.dreamconfig.DeSerializer.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamconfig.Enum.StorageType;
import com.dreamfirestudios.dreamconfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamconfig.Serializer.SerializerHelpers;
import lombok.Getter;
import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Function;

/**
 * <summary>
 * HashMap wrapper with enum-safe key (de)serialization.
 * </summary>
 * <remarks>
 * - Enum keys are serialized as {@code name()} (String) to avoid SnakeYAML global tags.
 * - On load, string keys are mapped back to enum constants; legacy enum-object keys are accepted.
 * </remarks>
 */
public class SaveableHashmap<K, V> {

    @Getter
    private HashMap<K, V> hashMap = new HashMap<>();
    private final Class<?> keyType;
    private final Class<?> dataType;

    public SaveableHashmap(Class<?> keyType, Class<?> dataType){
        this.keyType = keyType;
        this.dataType = dataType;
    }

    /**
     * <summary>Serialize map to a String→Object map, converting enum keys to {@code name()}.</summary>
     * <param name="saveConfigSingle">Framework serializer for values</param>
     * <returns>LinkedHashMap with YAML-safe keys</returns>
     */
    public LinkedHashMap<Object, Object> Serialize(Function<Object, Object> saveConfigSingle){
        var result  = new LinkedHashMap<Object, Object>();
        for (var entry : hashMap.entrySet()) {
            var keyObj = entry.getKey();
            Object safeKey = (keyObj != null && keyType.isEnum())
                    ? ((Enum<?>) keyObj).name()
                    : saveConfigSingle.apply(keyObj);
            result.put(safeKey, saveConfigSingle.apply(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void DeSerialiseData(StorageType saveableType, HashMap<Object, Object> configData) throws Exception {
        for (var key : configData.keySet()){
            var dx = DeSerialiseKey(saveableType, key, keyType);
            var dy = DeSerialiseValue(saveableType, configData.get(key), dataType);
            hashMap.put(dx, dy);
        }
    }

    @SuppressWarnings("unchecked")
    public void DeSerialiseData(StorageType saveableType, Document document) throws Exception {
        for (var key : document.keySet()){
            var dx = DeSerialiseKey(saveableType, key, keyType);
            var dy = DeSerialiseValue(saveableType, document.get(key), dataType);
            hashMap.put(dx, dy);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private K DeSerialiseKey(StorageType storageType, Object source, Class<?> targetType) throws Exception {
        if (IPulseClass.class.isAssignableFrom(targetType)) {
            IPulseClass pulseClassInstance = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(targetType);
            pulseClassInstance.BeforeLoadConfig();
            Object deserialized = null;
            if (storageType == StorageType.CONFIG) {
                deserialized = DreamConfigDeSerializer.ReturnAllClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            }
            pulseClassInstance.AfterLoadConfig();
            return (K) deserialized;
        }

        if (targetType.isEnum()) {
            if (source instanceof Enum<?> e && targetType.isInstance(e)) {
                return (K) e; // legacy path
            }
            String name = String.valueOf(source);
            try {
                return (K) Enum.valueOf((Class<? extends Enum>) targetType, name);
            } catch (IllegalArgumentException ex) {
                // Unknown enum constant — fall through
            }
        }

        if (storageType == StorageType.CONFIG) {
            return (K) DreamConfigDeSerializer.LoadConfigSingle(targetType, source, source);
        }
        throw new IllegalArgumentException("Unsupported key type for deserialization.");
    }

    @SuppressWarnings("unchecked")
    private V DeSerialiseValue(StorageType storageType, Object source, Class<?> targetType) throws Exception {
        if (IPulseClass.class.isAssignableFrom(targetType)) {
            IPulseClass pulseClassInstance = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(targetType);
            pulseClassInstance.BeforeLoadConfig();
            Object deserialized = null;
            if (storageType == StorageType.CONFIG) {
                deserialized = DreamConfigDeSerializer.ReturnAllClassFields((HashMap<Object, Object>) source, pulseClassInstance.getClass(), pulseClassInstance);
            }
            pulseClassInstance.AfterLoadConfig();
            return (V) deserialized;
        } else {
            if (storageType == StorageType.CONFIG) {
                return (V) DreamConfigDeSerializer.LoadConfigSingle(dataType, source, source);
            }
        }
        throw new IllegalArgumentException("Unsupported value type for deserialization.");
    }
}