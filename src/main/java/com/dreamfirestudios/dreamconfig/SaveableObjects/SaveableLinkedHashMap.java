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
 * LinkedHashMap wrapper with enum-safe key (de)serialization.
 * </summary>
 * <remarks>
 * - Keys that are enums are serialized as their {@code name()} (String) to avoid SnakeYAML global tags.
 * - On load, string keys are mapped back to enum constants; legacy enum-object keys are also accepted.
 * </remarks>
 */
public class SaveableLinkedHashMap<K, V> {

    @Getter
    private final LinkedHashMap<K, V> hashMap = new LinkedHashMap<>();
    private final Class<?> keyType;
    private final Class<?> dataType;

    public SaveableLinkedHashMap(Class<?> keyType, Class<?> valueType) {
        this.keyType = keyType;
        this.dataType = valueType;
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
            // Enum keys → write as String name()
            Object safeKey = (keyObj != null && keyType.isEnum())
                    ? ((Enum<?>) keyObj).name()
                    : saveConfigSingle.apply(keyObj);
            result.put(safeKey, saveConfigSingle.apply(entry.getValue()));
        }
        return result;
    }

    /**
     * <summary>Deserialize from a raw HashMap produced by config storage.</summary>
     */
    @SuppressWarnings("unchecked")
    public void DeSerialiseData(StorageType saveableType, HashMap<Object, Object> configData) throws Exception {
        for (var key : configData.keySet()){
            var dx = DeSerialiseKey(saveableType, key, keyType);
            var dy = DeSerialiseValue(saveableType, configData.get(key), dataType);
            hashMap.put(dx, dy);
        }
    }

    /**
     * <summary>Deserialize from a Mongo Document (if used).</summary>
     */
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
        // Pulse class keys
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

        // Enum keys (accept new String + legacy enum object)
        if (targetType.isEnum()) {
            if (source instanceof Enum<?> e && targetType.isInstance(e)) {
                return (K) e; // legacy path (if any)
            }
            String name = String.valueOf(source);
            try {
                return (K) Enum.valueOf((Class<? extends Enum>) targetType, name);
            } catch (IllegalArgumentException ex) {
                // Unknown enum constant: fall through to framework for best-effort, else fail below
            }
        }

        // Fallback to framework loader
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