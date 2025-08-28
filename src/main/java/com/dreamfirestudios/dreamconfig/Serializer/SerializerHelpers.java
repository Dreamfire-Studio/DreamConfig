package com.dreamfirestudios.dreamconfig.Serializer;

import com.dreamfirestudios.dreamconfig.Abstract.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamconfig.Interface.DontDefault;
import com.dreamfirestudios.dreamconfig.Interface.DontSave;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

public class SerializerHelpers {
    public static final java.text.SimpleDateFormat SimpleDateFormat =
            new SimpleDateFormat("yyyy-MMM-dd-HH-mm-ss", Locale.ENGLISH);

    /// <summary>
    /// Returns all serializable fields for the given type/object.
    /// Now includes private/protected fields (uses setAccessible(true)),
    /// skips static, transient and synthetic fields, and still honours @DontSave.
    /// </summary>
    /// <param name="parentClass">Type being inspected</param>
    /// <param name="object">Instance to read defaults from</param>
    /// <returns>Map of Field -> current/default value (non-null)</returns>
    public static LinkedHashMap<Field, Object> ReturnAllFields(Class<?> parentClass, Object object) throws IllegalAccessException {
        final LinkedHashMap<Field, Object> data = new LinkedHashMap<>();

        for (Field field : parentClass.getDeclaredFields()) {
            // skip explicit opt-outs and non-instance stuff
            if (field.isAnnotationPresent(DontSave.class)) continue;
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || field.isSynthetic()) continue;

            // allow private/protected
            if (!field.canAccess(object)) field.setAccessible(true);

            Object stored = field.get(object);

            // default missing values unless @DontDefault
            if (stored == null && !field.isAnnotationPresent(DontDefault.class)) {
                var variableTest = DreamVariableTestAPI.returnTestFromType(field.getType());
                if (field.getType() == Date.class) {
                    stored = new Date();
                } else if (variableTest != null) {
                    stored = variableTest.ReturnDefaultValue();
                } else {
                    stored = CreateClassInstanceBlank(field.getType());
                }
            }
            if (stored != null) data.put(field, stored);
        }

        // Preserve existing behaviour for StaticEnumPulseConfig (pull fields from super chain once)
        if (object instanceof StaticEnumPulseConfig) {
            Class<?> superClass = parentClass.getSuperclass();
            if (superClass != null && StaticEnumPulseConfig.class.isAssignableFrom(superClass)) {
                var inherited = ReturnAllFields(superClass, object);
                for (var e : inherited.entrySet()) data.put(e.getKey(), e.getValue());
            }
        }
        return data;
    }

    public static Object CreateClassInstanceBlank(Class<?> instanceClass){
        try { return instanceClass.getConstructor().newInstance(); }
        catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ignored) {
            return null;
        }
    }

    public static Object CreateInstanceWithID(String fileName, Class<?> instanceClass){
        try {
            return instanceClass.getConstructor(String.class).newInstance(fileName);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ignored) {
            try { return instanceClass.getConstructor().newInstance(); }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                return null;
            }
        }
    }
}