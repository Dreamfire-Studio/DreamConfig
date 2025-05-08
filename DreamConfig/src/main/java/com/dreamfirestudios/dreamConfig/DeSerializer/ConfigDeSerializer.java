package com.dreamfirestudios.dreamConfig.DeSerializer;

import com.dreamfirestudios.dreamConfig.Abstract.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamConfig.Enum.StorageType;
import com.dreamfirestudios.dreamConfig.Interface.*;
import com.dreamfirestudios.dreamConfig.Object.ConfigObject;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamConfig.Serializer.ConfigSerializer;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import com.dreamfirestudios.dreamCore.DreamfireVariable.DreamfireVariable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

public class ConfigDeSerializer {
    public static void LoadConfig(IPulseConfig pulseConfig, ConfigObject configObject) throws Exception {
        pulseConfig.BeforeLoadConfig();
        var storedData = configObject.HashMap(pulseConfig.documentID(), true);
        var data = ReturnClassFields(storedData, pulseConfig.getClass(), pulseConfig);
        pulseConfig.AfterLoadConfig();
    }

    public static Object ReturnClassFields(HashMap<Object, Object> configData, Class<?> parentClass, Object object) throws Exception {
        var dataFields = SerializerHelpers.ReturnAllFields(parentClass, object);
        for(var field : dataFields.keySet()){
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            if(!configData.containsKey(fieldName)){
                field.set(object, ConfigSerializer.SaveConfigSingle(dataFields.get(field)));
            }
            else {
                var fieldValue = configData.get(fieldName);
                if (field.isAnnotationPresent(Encrypt.class)) fieldValue = decryptData(fieldValue);
                var deSerialisedData = LoadConfigSingle(dataFields.get(field).getClass(), dataFields.get(field), fieldValue);
                try{field.set(object, deSerialisedData);}
                catch(Exception ignored) {field.set(object, dataFields.get(field));}
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

    public static Object LoadConfigSingle(Class<?> classDataType, Object classData, Object configData) throws Exception {
        if(classData == null || configData == null) return null;

        var variableTest = DreamfireVariable.returnTestFromType(classDataType);
        if (classDataType.isAnnotationPresent(CustomDeserialize.class)) {
            var annotation = classDataType.getAnnotation(CustomDeserialize.class);
            var strategy = annotation.strategy().getDeclaredConstructor().newInstance();
            return strategy.deserialize(configData);
        }

        if(ConfigurationSerialization.class.isAssignableFrom(classDataType)){
            return configData;
        }else if(IPulseClass.class.isAssignableFrom(classDataType)){
            var pulseClass = (IPulseClass) classData;
            pulseClass.BeforeLoadConfig();
            var data = ReturnClassFields((HashMap<Object, Object>) configData, pulseClass.getClass(), pulseClass);
            pulseClass.AfterLoadConfig();
            return data;
        }else if(SaveableHashmap.class.isAssignableFrom(classDataType)){
            var saveableHashmap = (SaveableHashmap) classData;
            saveableHashmap.getHashMap().clear();
            saveableHashmap.DeSerialiseData(StorageType.CONFIG, (HashMap<Object, Object>) configData);
            return saveableHashmap;
        }else if(SaveableLinkedHashMap.class.isAssignableFrom(classDataType)){
            var saveableLinkedHashMap = (SaveableLinkedHashMap) classData;
            saveableLinkedHashMap.getHashMap().clear();
            saveableLinkedHashMap.deserializeFromConfig(StorageType.CONFIG, (HashMap<Object, Object>) configData);
            return saveableLinkedHashMap;
        }else if(SaveableArrayList.class.isAssignableFrom(classDataType)) {
            var saveableArrayList = (SaveableArrayList) classData;
            saveableArrayList.getArrayList().clear();
            saveableArrayList.DeSerialiseData(StorageType.CONFIG, (ArrayList<Object>) configData);
            return saveableArrayList;
        }else if(ICustomVariable.class.isAssignableFrom(classDataType)){
            var customVariable = (ICustomVariable) classDataType.getConstructor().newInstance();
            var hashMap = (HashMap<Object, Object>) configData;
            customVariable.BeforeLoad();
            customVariable.DeSerializeData(hashMap);
            customVariable.AfterLoad();
            return customVariable;
        }else if(Date.class.isAssignableFrom(classDataType)){
            return SerializerHelpers.SimpleDateFormat.parse(configData.toString());
        }else if(variableTest != null){
            return variableTest.DeSerializeData(configData);
        }
        return configData;
    }
}
