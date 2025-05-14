package com.dreamfirestudios.dreamConfig.Serializer;

import com.dreamfirestudios.dreamConfig.Interface.*;
import com.dreamfirestudios.dreamConfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamConfig.SaveableObjects.ICustomVariable;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamCore.DreamfireVariable.DreamfireVariable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.LinkedHashMap;

public class DreamConfigSerializer {
    public static void SaveDreamConfig(IDreamConfig iDreamConfig, DreamConfigObject dreamConfigObject) throws Exception {
        iDreamConfig.BeforeSaveConfig();
        if(iDreamConfig.getClass().isAnnotationPresent(DreamConfigHeader.class)) dreamConfigObject.setHeader(iDreamConfig.getClass().getAnnotation(DreamConfigHeader.class));
        dreamConfigObject.set(iDreamConfig.documentID(), ReturnAllClassFields(iDreamConfig.getClass(), iDreamConfig));
        if(iDreamConfig.getClass().isAnnotationPresent(DreamConfigFooter.class)) dreamConfigObject.setFooter(iDreamConfig.getClass().getAnnotation(DreamConfigFooter.class));
        iDreamConfig.AfterSaveConfig();
    }

    public static LinkedHashMap<String, Object>  ReturnAllClassFields(Class<?> parentClass, Object object) throws Exception {
        var storedData = new LinkedHashMap<String, Object>();
        var dataFields = SerializerHelpers.ReturnAllFields(parentClass, object);
        for(var field : dataFields.keySet()){
            var fieldComment = field.isAnnotationPresent(StorageComment.class) ? field.getAnnotation(StorageComment.class).value() : "";
            if(!fieldComment.isEmpty()) storedData.put(String.format("# +------------------%s", fieldComment), "------------------+ #");
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            if(fieldName.isEmpty()) fieldName = field.getName();
            var fieldValue = SaveConfigSingle(dataFields.get(field));
            storedData.put(fieldName, fieldValue);
        }
        return storedData;
    }

    public static Object SaveConfigSingle(Object storedData) throws Exception {
        if(storedData == null) return null;
        var classDataType = storedData.getClass();
        var variableTest = DreamfireVariable.returnTestFromType(classDataType);

        if (classDataType.isAnnotationPresent(CustomSerialize.class)) {
            var annotation = classDataType.getAnnotation(CustomSerialize.class);
            var strategy = annotation.serializationStrategy().getDeclaredConstructor().newInstance();
            return strategy.serialize(storedData);
        }

        if(storedData instanceof IPulseClass pulseClass){
            pulseClass.BeforeSaveConfig();
            var data = ReturnAllClassFields(pulseClass.getClass(), pulseClass);
            pulseClass.AfterSaveConfig();
            return data;
        }else if(storedData instanceof SaveableHashmap<?, ?> saveableHashmap){
            return saveableHashmap.Serialize(obj -> {
                try {return SaveConfigSingle(obj);}
                catch (Exception e) {throw new RuntimeException(e);}
            });
        }else if(storedData instanceof SaveableLinkedHashMap<?, ?> saveableLinkedHashMap){
            return saveableLinkedHashMap.Serialize(obj -> {
                try {return SaveConfigSingle(obj);}
                catch (Exception e) {throw new RuntimeException(e);}
            });
        }else if(storedData instanceof SaveableArrayList<?> saveableArrayList){
            return saveableArrayList.Serialize(obj -> {
                try {return SaveConfigSingle(obj);}
                catch (Exception e) {throw new RuntimeException(e);}
            });
        }else if(storedData instanceof ICustomVariable iCustomVariable){
            iCustomVariable.BeforeSave();
            var data = iCustomVariable.SerializeData();
            iCustomVariable.AfterSave();
            return data;
        }else if(ConfigurationSerialization.class.isAssignableFrom(storedData.getClass())) return storedData;
        else if(storedData instanceof Date date) return SerializerHelpers.SimpleDateFormat.format(date);
        else if(variableTest != null) return variableTest.SerializeData(storedData);
        else return storedData;
    }
}
