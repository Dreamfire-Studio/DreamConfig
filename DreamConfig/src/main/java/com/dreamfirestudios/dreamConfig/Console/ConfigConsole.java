package com.dreamfirestudios.dreamConfig.Console;

import com.dreamfirestudios.dreamConfig.Interface.ICustomVariable;
import com.dreamfirestudios.dreamConfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamConfig.Interface.IPulseConfig;
import com.dreamfirestudios.dreamConfig.Interface.SaveName;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import com.dreamfirestudios.dreamCore.DreamfireVariable.DreamfireVariable;

import java.util.Date;

public class ConfigConsole {
    private static String ReturnIndent(int indent){
        return " ".repeat(Math.max(0, indent));
    }

    public static String ConsoleOutput(IPulseConfig pulseConfig) throws Exception {
        var stringBuilder = new StringBuilder(String.format("==========[%s / PULSE CONFIG]==========\n{\n", pulseConfig.documentID()));
        var dataFields = SerializerHelpers.ReturnAllFields(pulseConfig.getClass(), pulseConfig);
        for(var field : dataFields.keySet()){
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            var fieldValue = ConsoleOutputSingle(dataFields.get(field), 2);
            if(fieldValue == null) continue;
            stringBuilder.append(String.format("%s%s:%s\n", ReturnIndent(1), fieldName, fieldValue.toString()));
        }
        stringBuilder.append("}\n==========[END]==========");
        return stringBuilder.toString();
    }

    private static String ConsoleOutputPulseClass(IPulseClass pulseClass, int indent) throws Exception {
        var stringBuilder = new StringBuilder("<");
        var dataFields = SerializerHelpers.ReturnAllFields(pulseClass.getClass(), pulseClass);
        for(var field : dataFields.keySet()){
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            var fieldValue = ConsoleOutputSingle(dataFields.get(field), 0);
            if(fieldValue == null) continue;
            stringBuilder.append(String.format("\n%s%s:%s", ReturnIndent(indent + 1), fieldName, fieldValue.toString()));
        }
        stringBuilder.append(String.format("\n%s>", ReturnIndent(indent - 1)));
        return stringBuilder.toString();
    }

    private static String ConsoleOutputSaveableHashmap(SaveableHashmap saveableHashmap, int indent) throws Exception {
        if(saveableHashmap.getHashMap().isEmpty()) return "{}";
        var stringBuilder = new StringBuilder("{");
        for(var key : saveableHashmap.getHashMap().keySet()){
            var skey = ConsoleOutputSingle(key, indent);
            var sValue = ConsoleOutputSingle(saveableHashmap.getHashMap().get(key), indent);
            stringBuilder.append(String.format("\n%s%s:%s", ReturnIndent(indent), skey, sValue));
        }
        stringBuilder.append(String.format("\n%s}", ReturnIndent(indent - 1)));
        return stringBuilder.toString();
    }

    private static String ConsoleOutputSaveableLinkedHashMap(SaveableLinkedHashMap saveableLinkedHashMap, int indent) throws Exception {
        if(saveableLinkedHashMap.getHashMap().isEmpty()) return "{}";
        var stringBuilder = new StringBuilder("{");
        for(var key : saveableLinkedHashMap.getHashMap().keySet()){
            var skey = ConsoleOutputSingle(key, indent);
            var sValue = ConsoleOutputSingle(saveableLinkedHashMap.getHashMap().get(key), indent);
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

    private static String ConsoleOutputSingle(Object storedData, int indent) throws Exception{
        if(storedData == null) return null;
        var variableTest = DreamfireVariable.returnTestFromType(storedData.getClass());
        if(storedData instanceof IPulseClass pulseClass){
            return ConsoleOutputPulseClass(pulseClass, indent + 1);
        }else if(storedData instanceof SaveableHashmap saveableHashmap){
            return ConsoleOutputSaveableHashmap(saveableHashmap, indent + 1);
        }else if(storedData instanceof SaveableLinkedHashMap saveableLinkedHashMap){
            return ConsoleOutputSaveableLinkedHashMap(saveableLinkedHashMap, indent + 1);
        }else if(storedData instanceof SaveableArrayList saveableArrayList){
            return ConsoleOutputSaveableArrayList(saveableArrayList, indent + 1);
        }else if(storedData instanceof ICustomVariable customVariable){
            return ConsoleOutputSaveableHashmap(customVariable.SerializeData(), indent + 1);
        }else if(storedData instanceof Date date){
            return date.toString();
        }else if(variableTest != null){
            return String.format("%s%s", ReturnIndent(indent), variableTest.SerializeData(storedData).toString());
        }else{
            return storedData.toString();
        }
    }
}
