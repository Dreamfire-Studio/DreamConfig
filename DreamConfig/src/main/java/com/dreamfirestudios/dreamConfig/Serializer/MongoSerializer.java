package com.dreamfirestudios.dreamConfig.Serializer;

import com.dreamfirestudios.dreamConfig.Interface.*;
import com.dreamfirestudios.dreamConfig.Object.MongoConnection;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfire.dreamfirecore.DreamfireVariable.DreamfireVariable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class MongoSerializer {
    public static <T extends IPulseMongo> void SaveMongo(T pulseMongo, MongoConnection mongoConnection, Consumer<T> onSuccess, Consumer<Throwable> onError) throws Exception{
        pulseMongo.BeforeSaveMongo();
        var convertedDocument = mongoConnection.defaultDocument(pulseMongo);
        var serializedDocument = ReturnClassFields(pulseMongo.getClass(), pulseMongo);
        for (var key : serializedDocument.keySet()) convertedDocument.put(key, serializedDocument.get(key));
        mongoConnection.insertOrReplace(pulseMongo.collectionName(), null, pulseMongo.documentID(), convertedDocument,
                ()->{
                    pulseMongo.AfterSaveMongo();
                    onSuccess.accept(pulseMongo);
                },
                onError
        );
    }

    public static LinkedHashMap<String, Object> ReturnClassFields(Class<?> parentClass, Object object) throws Exception {
        var storedData = new LinkedHashMap<String, Object>();
        var dataFields = SerializerHelpers.ReturnAllFields(parentClass, object);
        for(var field : dataFields.keySet()){
            var fieldComment = field.isAnnotationPresent(StorageComment.class) ? field.getAnnotation(StorageComment.class).value() : "";
            if(!fieldComment.isEmpty()) storedData.put(String.format("# +------------------%s", fieldComment), "------------------+ #");
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            if(fieldName.isEmpty()) fieldName = field.getName();
            var fieldValue = SaveMongoSingle(dataFields.get(field));
            if(field.isAnnotationPresent(Encrypt.class)) fieldValue = encryptData(fieldValue);
            storedData.put(fieldName, fieldValue);
        }
        return storedData;
    }

    public static Object encryptData(Object data) {
        if (data == null) return null;
        var rawData = data.toString();
        return Base64.getEncoder().encodeToString(rawData.getBytes());
    }

    public static Object SaveMongoSingle(Object storedData) throws Exception {
        if(storedData == null) return null;

        var classDataType = storedData.getClass();
        var variableTest = DreamfireVariable.returnTestFromType(classDataType);
        if (classDataType.isAnnotationPresent(CustomSerialize.class)) {
            var annotation = classDataType.getAnnotation(CustomSerialize.class);
            var strategy = annotation.strategy().getDeclaredConstructor().newInstance();
            return strategy.serialize(storedData);
        }

        if(ConfigurationSerialization.class.isAssignableFrom(storedData.getClass())){
            return null;
        }else if(storedData instanceof IPulseClass pulseClass){
            pulseClass.BeforeSaveConfig();
            var data = ReturnClassFields(pulseClass.getClass(), pulseClass);
            pulseClass.AfterSaveConfig();
            return data;
        }else if(storedData instanceof SaveableHashmap saveableHashmap){
            var returnData = new LinkedHashMap<>();
            for(var key : saveableHashmap.getHashMap().keySet()){
                var value = saveableHashmap.getHashMap().get(key);
                returnData.put(SaveMongoSingle(key).toString(), SaveMongoSingle(value));
            }
            return returnData;
        }else if(storedData instanceof SaveableLinkedHashMap saveableLinkedHashMap){
            var returnData = new LinkedHashMap<>();
            for(var key : saveableLinkedHashMap.getHashMap().keySet()){
                var value = saveableLinkedHashMap.getHashMap().get(key);
                returnData.put(SaveMongoSingle(key).toString(), SaveMongoSingle(value));
            }
            return returnData;
        }else if(storedData instanceof SaveableArrayList saveableArrayList){
            var returnData = new ArrayList<>();
            for(var key : saveableArrayList.getArrayList()) returnData.add(SaveMongoSingle(key));
            return returnData;
        }else if(storedData instanceof ICustomVariable customVariable){
            customVariable.BeforeSave();
            var data = customVariable.SerializeData();
            customVariable.AfterSave();
            var returnData = new LinkedHashMap<>();
            for(var key : data.getHashMap().keySet()){
                var value = data.getHashMap().get(key);
                returnData.put(SaveMongoSingle(key), SaveMongoSingle(value));
            }
            return returnData;
        }
        else if(storedData instanceof Date date) return SerializerHelpers.SimpleDateFormat.format(date);
        else if(variableTest != null) return variableTest.SerializeData(storedData);
        return storedData;
    }
}
