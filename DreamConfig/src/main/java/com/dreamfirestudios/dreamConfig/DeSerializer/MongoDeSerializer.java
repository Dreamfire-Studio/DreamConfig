package com.dreamfirestudios.dreamConfig.DeSerializer;

import com.dreamfirestudios.dreamConfig.Enum.StorageType;
import com.dreamfirestudios.dreamConfig.Interface.*;
import com.dreamfirestudios.dreamConfig.Object.MongoConnection;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import com.dreamfirestudios.dreamCore.DreamfireVariable.DreamfireVariable;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.*;
import java.util.function.Consumer;

public class MongoDeSerializer {
    public static <T extends IPulseMongo> void LoadMongo(T pulseMongo, MongoConnection mongoConnection, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        pulseMongo.BeforeLoadMongo();
        mongoConnection.getOne(pulseMongo.collectionName(), null, pulseMongo.documentID(), storedData -> {
            try {
                if (storedData != null)ReturnClassFieldsMap(storedData, pulseMongo.getClass(), pulseMongo);
                pulseMongo.AfterLoadMongo();
                onSuccess.accept(pulseMongo);
            } catch (Exception e) {
                onError.accept(e);
            }
        }, onError);
    }

    public static Object ReturnClassFieldsMap(Map configData, Class<?> parentClass, Object object) throws Exception {
        var dataFields = SerializerHelpers.ReturnAllFields(parentClass, object);

        for (var field : dataFields.keySet()) {
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            if (!configData.containsKey(fieldName)) field.set(object, null);
            else {
                var fieldValue = configData.get(fieldName);
                if (field.isAnnotationPresent(Encrypt.class)) fieldValue = decryptData(fieldValue);
                field.set(object, LoadMongoSingle(dataFields.get(field).getClass(), dataFields.get(field), fieldValue));
            }
        }

        return object;
    }

    public static Object decryptData(Object encryptedData) {
        if (encryptedData == null) return null;
        String rawEncryptedData = encryptedData.toString();
        byte[] decodedBytes = Base64.getDecoder().decode(rawEncryptedData);
        String decryptedData = new String(decodedBytes);
        return decryptedData;
    }

    public static Object LoadMongoSingle(Class<?> classDataType, Object classData, Object configData) throws Exception {
        if(classData == null || configData == null){ return null; }

        var variableTest = DreamfireVariable.returnTestFromType(classDataType);
        if (classDataType.isAnnotationPresent(CustomDeserialize.class)) {
            var annotation = classDataType.getAnnotation(CustomDeserialize.class);
            var strategy = annotation.strategy().getDeclaredConstructor().newInstance();
            return strategy.deserialize(configData);
        }

        if(ConfigurationSerialization.class.isAssignableFrom(classDataType)){
            return null;
        } else if(IPulseClass.class.isAssignableFrom(classDataType)){
            var pulseClass = (IPulseClass) classData;
            pulseClass.BeforeLoadConfig();
            var data = ReturnClassFieldsMap((Document) configData, pulseClass.getClass(), pulseClass);
            pulseClass.AfterLoadConfig();
            return data;
        } else if(SaveableHashmap.class.isAssignableFrom(classDataType)){
            var saveableHashmap = (SaveableHashmap) classData;
            saveableHashmap.DeSerialiseData(StorageType.MONGO, (Document) configData);
            return saveableHashmap;
        } else if(SaveableLinkedHashMap.class.isAssignableFrom(classDataType)){
            var saveableLinkedHashMap = (SaveableLinkedHashMap) classData;
            saveableLinkedHashMap.deserializeFromDocument(StorageType.MONGO, (Document) configData);
            return saveableLinkedHashMap;
        } else if(SaveableArrayList.class.isAssignableFrom(classDataType)) {
            var saveableArrayList = (SaveableArrayList) classData;
            saveableArrayList.DeSerialiseData(StorageType.MONGO, (List<Object>) configData);
            return saveableArrayList;
        } else if(ICustomVariable.class.isAssignableFrom(classDataType)){
            var customVariable = (ICustomVariable) classDataType.getConstructor().newInstance();
            var hashMap = new HashMap<Object, Object>();
            var document = (Document) configData;
            for(var x : document.keySet()) hashMap.put(x, document.get(x));
            customVariable.BeforeLoad();
            customVariable.DeSerializeData(hashMap);
            customVariable.AfterLoad();
            return customVariable;
        } else if(Date.class.isAssignableFrom(classDataType)){
            return SerializerHelpers.SimpleDateFormat.parse(configData.toString());
        } else if(variableTest != null){
            return variableTest.DeSerializeData(configData);
        }
        return configData;
    }

}
