package com.dreamfirestudios.dreamconfig.DeSerializer;

import com.dreamfirestudios.dreamconfig.Enum.StorageType;
import com.dreamfirestudios.dreamconfig.Interface.CustomSerialize;
import com.dreamfirestudios.dreamconfig.Interface.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamconfig.Interface.SaveName;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamconfig.Interface.ICustomVariable;
import com.dreamfirestudios.dreamconfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamconfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamconfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamconfig.Serializer.SerializerHelpers;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class DreamConfigDeSerializer {
    public static void LoadDreamConfig(IDreamConfig iDreamConfig, DreamConfigObject dreamConfigObject) throws Exception {
        iDreamConfig.BeforeLoadConfig();
        var storedData = dreamConfigObject.HashMap(iDreamConfig.documentID(), true);
        var data = ReturnAllClassFields(storedData, iDreamConfig.getClass(), iDreamConfig);
        iDreamConfig.AfterLoadConfig();
    }

    public static Object ReturnAllClassFields(HashMap<Object, Object> configData, Class<?> parentClass, Object object) throws Exception {
        var dataFields = SerializerHelpers.ReturnAllFields(parentClass, object);
        for(var field : dataFields.keySet()){
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            if(fieldName.isEmpty()) fieldName = field.getName();
            if(configData.containsKey(fieldName)){
                var fieldValue = configData.get(fieldName);
                var deSerialisedData = LoadConfigSingle(dataFields.get(field).getClass(), dataFields.get(field), fieldValue);
                try{field.set(object, deSerialisedData);}
                catch(Exception ignored) {field.set(object, dataFields.get(field));}
            }
        }
        return object;
    }

    public static Object LoadConfigSingle(Class<?> classDataType, Object classData, Object configData) throws Exception {
        if(classData == null || configData == null) return null;
        var variableTest = DreamVariableTestAPI.returnTestFromType(classDataType);

        if (classDataType.isAnnotationPresent(CustomSerialize.class)) {
            var annotation = classDataType.getAnnotation(CustomSerialize.class);
            var strategy = annotation.deserializationStrategy().getDeclaredConstructor().newInstance();
            return strategy.deserialize(configData);
        }

        if(IPulseClass.class.isAssignableFrom(classDataType)){
            var pulseClass = (IPulseClass) classData;
            pulseClass.BeforeLoadConfig();
            var data = ReturnAllClassFields((HashMap<Object, Object>) configData, pulseClass.getClass(), pulseClass);
            pulseClass.AfterLoadConfig();
            return data;
        }else if(SaveableHashmap.class.isAssignableFrom(classDataType)){
            var saveableHashmap = (SaveableHashmap) classData;
            saveableHashmap.DeSerialiseData(StorageType.CONFIG, (HashMap<Object, Object>) configData);
            return saveableHashmap;
        }else if(SaveableLinkedHashMap.class.isAssignableFrom(classDataType)){
            var saveableLinkedHashMap = (SaveableLinkedHashMap) classData;
            saveableLinkedHashMap.DeSerialiseData(StorageType.CONFIG, (HashMap<Object, Object>) configData);
            return saveableLinkedHashMap;
        }else if(SaveableArrayList.class.isAssignableFrom(classDataType)) {
            var saveableArrayList = (SaveableArrayList) classData;
            saveableArrayList.DeSerialiseData(StorageType.CONFIG, (ArrayList<Object>) configData);
            return saveableArrayList;
        }else if(ICustomVariable.class.isAssignableFrom(classDataType)){
            var customVariable = (ICustomVariable) classDataType.getConstructor().newInstance();
            var hashMap = (LinkedHashMap<Object, Object>) configData;
            customVariable.BeforeLoad();
            customVariable.DeSerializeData(hashMap);
            customVariable.AfterLoad();
            return customVariable;
        }else if(Date.class.isAssignableFrom(classDataType)){
            return SerializerHelpers.SimpleDateFormat.parse(configData.toString());
        }else if(variableTest != null){
            return variableTest.DeSerializeData(configData);
        }else{
            return configData;
        }
    }
}