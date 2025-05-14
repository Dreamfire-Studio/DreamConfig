package com.dreamfirestudios.dreamConfig.API;

import com.dreamfirestudios.dreamConfig.DeSerializer.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamConfig.Interface.IDreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamConfig.Interface.SaveName;
import com.dreamfirestudios.dreamConfig.Interface.StoragePath;
import com.dreamfirestudios.dreamConfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamConfig.SaveableObjects.ICustomVariable;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamConfig.Serializer.DreamConfigSerializer;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import com.dreamfirestudios.dreamCore.DreamfireFile.DreamfireDir;
import com.dreamfirestudios.dreamCore.DreamfireVariable.DreamfireVariable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DreamConfigAPI {

    public static CompletableFuture<HashMap<String, IDreamConfig>> ReturnAllConfigDocuments(JavaPlugin javaPlugin, IDreamConfig iPulseConfig) {
        return CompletableFuture.supplyAsync(() -> {
            var data = new HashMap<String, IDreamConfig>();
            try {
                for (var file : DreamfireDir.returnAllFilesFromDirectory(new File(GetDreamConfigPath(iPulseConfig)), false)) {
                    if (!file.getName().contains(".yml")) continue;
                    var fileName = file.getName().replace(".yml", "");
                    var newInstance = SerializerHelpers.CreateInstanceWithID(fileName, iPulseConfig.getClass());
                    if (newInstance == null) continue;
                    var pc = (IDreamConfig) newInstance;
                    LoadDreamConfig(javaPlugin, pc, loaded -> {}).join();
                    data.put(fileName, pc);
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            return data;
        });
    }

    public static <T extends IDreamConfig> void SaveDreamConfig(JavaPlugin javaPlugin, T iDreamConfig, Consumer<T> onSuccessSave){
        CompletableFuture.runAsync(() -> {
            var dreamConfigPath = GetDreamConfigPath(iDreamConfig);
            var documentID = iDreamConfig.documentID();
            var dreamConfigObject = new DreamConfigObject(javaPlugin, dreamConfigPath, documentID);
            if (dreamConfigObject.IsFirstLoad()) iDreamConfig.FirstLoadConfig();
            try {
                DreamConfigSerializer.SaveDreamConfig(iDreamConfig, dreamConfigObject);
                onSuccessSave.accept(iDreamConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T extends IDreamConfig> CompletableFuture<Void> LoadDreamConfig(JavaPlugin javaPlugin, T iDreamConfig, Consumer<T> onSuccess){
        return CompletableFuture.runAsync(() -> {
            var dreamConfigPath = GetDreamConfigPath(iDreamConfig);
            var documentID = iDreamConfig.documentID();
            var dreamConfigObject = new DreamConfigObject(javaPlugin, dreamConfigPath, documentID);
            if (dreamConfigObject.IsFirstLoad()) {
                iDreamConfig.FirstLoadConfig();
                SaveDreamConfig(javaPlugin, iDreamConfig, onSuccess);
            } else {
                try {
                    DreamConfigDeSerializer.LoadDreamConfig(iDreamConfig, dreamConfigObject);
                    onSuccess.accept(iDreamConfig);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static <T extends IDreamConfig> void DisplayDreamConfig(T iDreamConfig, Consumer<T> onSuccess) {
        CompletableFuture.runAsync(() -> {
            try {
                var stringBuilder = new StringBuilder(String.format("==========[%s / PULSE CONFIG]==========\n{\n", iDreamConfig.documentID()));
                var dataFields = SerializerHelpers.ReturnAllFields(iDreamConfig.getClass(), iDreamConfig);
                for (var field : dataFields.keySet()) {
                    var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
                    var fieldValue = ConsoleOutputSingle(dataFields.get(field), 2);
                    if (fieldValue == null) continue;
                    stringBuilder.append(String.format("%s%s:%s\n", ReturnIndent(1), fieldName, fieldValue.toString()));
                }
                stringBuilder.append("}\n==========[END]==========");
                DreamfireChat.SendMessageToConsole(stringBuilder.toString());
                onSuccess.accept(iDreamConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T extends IDreamConfig> CompletableFuture<Void> DeleteDreamConfig(JavaPlugin javaPlugin, T iDreamConfig, Consumer<T> onSuccess) {
        return CompletableFuture.runAsync(() -> {
            var dreamConfigPath = GetDreamConfigPath(iDreamConfig);
            var documentID = iDreamConfig.documentID();
            var dreamConfigObject = new DreamConfigObject(javaPlugin, dreamConfigPath, documentID);
            dreamConfigObject.DeleteConfig();
            onSuccess.accept(iDreamConfig);
        });
    }

    private static String GetDreamConfigPath(IDreamConfig iDreamConfig){
        if(iDreamConfig.getClass().isAnnotationPresent(StoragePath.class)) return iDreamConfig.getClass().getAnnotation(StoragePath.class).value();
        return iDreamConfig.useSubFolder() ? iDreamConfig.documentID() : "";
    }

    private static String ConsoleOutputSingle(Object storedData, int indent) throws Exception{
        if(storedData == null) return null;
        var variableTest = DreamfireVariable.returnTestFromType(storedData.getClass());
        if(storedData instanceof IPulseClass pulseClass){
            return ConsoleOutputPulseClass(pulseClass, indent + 1);
        }else if(storedData instanceof SaveableHashmap saveableHashmap){
            return ConsoleOutputSaveableHashmap(saveableHashmap.getHashMap(), indent + 1);
        }else if(storedData instanceof SaveableLinkedHashMap saveableLinkedHashMap){
            return ConsoleOutputSaveableLinkedHashMap(saveableLinkedHashMap.getHashMap(), indent + 1);
        }else if(storedData instanceof SaveableArrayList saveableArrayList){
            return ConsoleOutputSaveableArrayList(saveableArrayList, indent + 1);
        }else if(storedData instanceof ICustomVariable customVariable){
            return ConsoleOutputSaveableLinkedHashMap(customVariable.SerializeData(), indent + 1);
        }else if(storedData instanceof Date date){
            return date.toString();
        }else if(variableTest != null){
            return String.format("%s%s", ReturnIndent(indent), variableTest.SerializeData(storedData).toString());
        }else{
            return storedData.toString();
        }
    }

    private static String ConsoleOutputPulseClass(IPulseClass pulseClass, int indent) throws Exception {
        var stringBuilder = new StringBuilder("<");
        var dataFields = SerializerHelpers.ReturnAllFields(pulseClass.getClass(), pulseClass);
        for(var field : dataFields.keySet()){
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            var fieldValue = ConsoleOutputSingle(dataFields.get(field), 0);
            if(fieldValue == null) continue;
            stringBuilder.append(String.format("\n%s%s:%s", ReturnIndent(indent + 1), fieldName, fieldValue));
        }
        stringBuilder.append(String.format("\n%s>", ReturnIndent(indent - 1)));
        return stringBuilder.toString();
    }

    private static String ConsoleOutputSaveableHashmap(HashMap<Object, Object> hashMap, int indent) throws Exception {
        if(hashMap.isEmpty()) return "{}";
        var stringBuilder = new StringBuilder("{");
        for(var key : hashMap.keySet()){
            var skey = ConsoleOutputSingle(key, indent);
            var sValue = ConsoleOutputSingle(hashMap.get(key), indent);
            stringBuilder.append(String.format("\n%s%s:%s", ReturnIndent(indent), skey, sValue));
        }
        stringBuilder.append(String.format("\n%s}", ReturnIndent(indent - 1)));
        return stringBuilder.toString();
    }

    private static String ConsoleOutputSaveableLinkedHashMap(LinkedHashMap<Object, Object> linkedHashMap, int indent) throws Exception {
        if(linkedHashMap.isEmpty()) return "{}";
        var stringBuilder = new StringBuilder("{");
        for(var key : linkedHashMap.keySet()){
            var skey = ConsoleOutputSingle(key, indent);
            var sValue = ConsoleOutputSingle(linkedHashMap.get(key), indent);
            stringBuilder.append(String.format("\n%s%s:%s", ReturnIndent(indent), skey, sValue));
        }
        stringBuilder.append(String.format("\n%s}", ReturnIndent(indent - 1)));
        return stringBuilder.toString();
    }

    private static String ConsoleOutputSaveableArrayList(SaveableArrayList saveableArrayList, int indent) throws Exception {
        if(saveableArrayList.getArrayList().isEmpty()) return "[]";

        var stringBuilder = new StringBuilder("[");
        for(var value : saveableArrayList.getArrayList()){
            var sValue = ConsoleOutputSingle(value, indent);
            stringBuilder.append(String.format("\n%s%s", ReturnIndent(indent), sValue));
        }

        stringBuilder.append(String.format("\n%s]", ReturnIndent(indent - 1)));
        return stringBuilder.toString();
    }

    private static String ReturnIndent(int indent){
        return " ".repeat(Math.max(0, indent));
    }
}
