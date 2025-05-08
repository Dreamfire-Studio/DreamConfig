package com.dreamfirestudios.dreamConfig.Serializer;

import com.dreamfirestudios.dreamConfig.Abstract.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamConfig.Interface.DontDefault;
import com.dreamfirestudios.dreamConfig.Interface.DontSave;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfire.dreamfirecore.DreamfireVariable.DreamfireVariable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

public class SerializerHelpers {
    public static final java.text.SimpleDateFormat SimpleDateFormat = new SimpleDateFormat("yyyy-MMM-dd-HH-mm-ss", Locale.ENGLISH);

    public static LinkedHashMap<Field, Object> ReturnAllFields(Class<?> parentClass, Object object) throws IllegalAccessException {
        var data = new LinkedHashMap<Field, Object>();
        for (var field : parentClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(DontSave.class)) continue;
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isPrivate(field.getModifiers())) continue;
            if (Modifier.isProtected(field.getModifiers())) continue;
            field.setAccessible(true);
            var storedData = field.get(object);
            if (storedData == null && !field.isAnnotationPresent(DontDefault.class)) {
                var variableTest = DreamfireVariable.returnTestFromType(field.getType());
                if (field.getType() == SaveableArrayList.class) storedData = new SaveableArrayList<>(Object.class);
                else if (field.getType() == SaveableHashmap.class) storedData = new SaveableHashmap<>(Object.class, Object.class);
                else if (field.getType() == SaveableLinkedHashMap.class) storedData = new SaveableLinkedHashMap<>(Object.class, Object.class);
                else if (field.getType() == Date.class) storedData = new Date();
                else if (variableTest != null) storedData = variableTest.ReturnDefaultValue();
                else storedData = CreateClassInstanceBlank(field.getType());
            }
            if (storedData != null) data.put(field, storedData);
        }
        if (object instanceof StaticEnumPulseConfig) {
            var superClass = parentClass.getSuperclass();
            if (superClass != null && StaticEnumPulseConfig.class.isAssignableFrom(superClass)) {
                var d = ReturnAllFields(superClass, object);
                for (var key : d.keySet()) {
                    data.put(key, d.get(key));
                }
            }
        }
        return data;
    }


    public static Object CreateInstanceWithID(String fileName, Class<?> instanceClass){
        try{
            return instanceClass.getConstructor(String.class).newInstance(fileName);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ignored) {
            try {
                return instanceClass.getConstructor().newInstance(); }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                return null;
            }
        }
    }

    public static Object CreateClassInstanceBlank(Class<?> instanceClass){
        try{
            return instanceClass.getConstructor().newInstance();
        }catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ignored) {
            return null;
        }
    }
}
