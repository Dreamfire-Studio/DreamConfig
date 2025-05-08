package com.dreamfirestudios.dreamConfig.Serializer;

import com.dreamfirestudios.dreamConfig.Abstract.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamConfig.Interface.*;
import com.dreamfirestudios.dreamConfig.Object.ConfigObject;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfire.dreamfirecore.DreamfireVariable.DreamfireVariable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;

public class ConfigSerializer {
    public static void SaveConfig(IPulseConfig pulseConfig, ConfigObject configObject) throws Exception {
        pulseConfig.BeforeSaveConfig();
        if(pulseConfig.getClass().isAnnotationPresent(ConfigHeader.class)) configObject.SetHeader(pulseConfig.getClass().getAnnotation(ConfigHeader.class));
        if(pulseConfig.getClass().isAnnotationPresent(ConfigFooter.class)) configObject.SetFooter(pulseConfig.getClass().getAnnotation(ConfigFooter.class));
        configObject.Set(pulseConfig.documentID(), ReturnClassFields(pulseConfig.getClass(), pulseConfig));
        pulseConfig.AfterSaveConfig();
    }

    public static LinkedHashMap<String, Object> ReturnClassFields(Class<?> parentClass, Object object) throws Exception {
        var storedData = new LinkedHashMap<String, Object>();
        var dataFields = SerializerHelpers.ReturnAllFields(parentClass, object);
        for(var field : dataFields.keySet()){
            var fieldComment = field.isAnnotationPresent(StorageComment.class) ? field.getAnnotation(StorageComment.class).value() : "";
            if(!fieldComment.isEmpty()) storedData.put(String.format("# +------------------%s", fieldComment), "------------------+ #");
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            if(fieldName.isEmpty()) fieldName = field.getName();
            var fieldValue = SaveConfigSingle(dataFields.get(field));
            if(field.isAnnotationPresent(Encrypt.class)) fieldValue = encryptData(fieldValue);
            storedData.put(fieldName, SaveConfigSingle(fieldValue));
        }
        return storedData;
    }

    //TODO add proper secruity encrypt
    public static Object encryptData(Object data) {
        if (data == null) return null;
        var rawData = data.toString();
        return Base64.getEncoder().encodeToString(rawData.getBytes());
    }

    public static Object SaveConfigSingle(Object storedData) throws Exception {
        if(storedData == null) return null;

        var classDataType = storedData.getClass();
        var variableTest = DreamfireVariable.returnTestFromType(classDataType);
        if (classDataType.isAnnotationPresent(CustomSerialize.class)) {
            var annotation = classDataType.getAnnotation(CustomSerialize.class);
            var strategy = annotation.strategy().getDeclaredConstructor().newInstance();
            return strategy.serialize(storedData);
        }

        if(storedData instanceof IPulseClass pulseClass){
            pulseClass.BeforeSaveConfig();
            var data = ReturnClassFields(pulseClass.getClass(), pulseClass);
            pulseClass.AfterSaveConfig();
            return data;
        }else if(storedData instanceof SaveableHashmap saveableHashmap){
            var returnData = new LinkedHashMap<>();
            for(var key : saveableHashmap.getHashMap().keySet()) returnData.put(SaveConfigSingle(key), SaveConfigSingle(saveableHashmap.getHashMap().get(key)));
            return returnData;
        }
        else if(storedData instanceof SaveableLinkedHashMap saveableLinkedHashMap){
            var returnData = new LinkedHashMap<>();
            for(var key : saveableLinkedHashMap.getHashMap().keySet()) returnData.put(SaveConfigSingle(key), SaveConfigSingle(saveableLinkedHashMap.getHashMap().get(key)));
            return returnData;
        }
        else if(storedData instanceof SaveableArrayList saveableArrayList){
            var returnData = new ArrayList<>();
            for(var key : saveableArrayList.getArrayList()) returnData.add(SaveConfigSingle(key));
            return returnData;
        }
        else if(storedData instanceof ICustomVariable customVariable){
            customVariable.BeforeSave();
            var data = customVariable.SerializeData();
            customVariable.AfterSave();
            var returnData = new LinkedHashMap<>();
            for(var key : data.getHashMap().keySet()) returnData.put(SaveConfigSingle(key), SaveConfigSingle(data.getHashMap().get(key)));
            return returnData;
        }
        else if(ConfigurationSerialization.class.isAssignableFrom(storedData.getClass())) return storedData;
        else if(storedData instanceof Date date) return SerializerHelpers.SimpleDateFormat.format(date);
        else if(variableTest != null) return variableTest.SerializeData(storedData);
        return storedData;
    }
}
