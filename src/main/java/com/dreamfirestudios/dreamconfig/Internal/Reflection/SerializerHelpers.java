/*
 * MIT License
 *
 * Copyright (c) 2025 Dreamfire Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dreamfirestudios.dreamconfig.Internal.Reflection;

import com.dreamfirestudios.dreamconfig.Model.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.DontDefault;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.DontSave;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.SaveName;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;

import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;

/// <summary>
/// Reflection utility helpers used across DreamConfig serialization and console rendering.
/// </summary>
/// <remarks>
/// Responsibilities:
/// - Collects serializable fields with support for default values.
/// - Instantiates objects with optional string IDs.
/// - Resolves persisted field names from annotations.
/// - Provides a shared standard date format.
/// <para/>
/// Fields marked with <see cref="DontSave"/> are ignored. Null fields are optionally
/// replaced with defaults unless annotated with <see cref="DontDefault"/>.
/// <see cref="StaticEnumPulseConfig"/> inheritance is also supported.
/// </remarks>
public final class SerializerHelpers {
    private SerializerHelpers() {}

    /// <summary>
    /// Standard date format for config serialization (<c>yyyy-MMM-dd-HH-mm-ss</c>, Locale.ENGLISH).
    /// </summary>
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MMM-dd-HH-mm-ss", Locale.ENGLISH);

    /// <summary>
    /// Collects all serializable fields on an object, applying defaults where allowed.
    /// </summary>
    /// <param name="parentClass">Class type to inspect.</param>
    /// <param name="instance">Object instance to read and mutate.</param>
    /// <returns>Ordered map of <see cref="Field"/> to field value.</returns>
    /// <remarks>
    /// - Skips <c>static</c>, <c>private</c>, <c>protected</c> fields.<br/>
    /// - Skips fields annotated <see cref="DontSave"/>.<br/>
    /// - Generates default values if null (unless <see cref="DontDefault"/> is present).<br/>
    /// - Includes parent fields when instance derives from <see cref="StaticEnumPulseConfig"/>.
    /// </remarks>
    /// <example>
    /// <code>
    /// LinkedHashMap&lt;Field,Object&gt; map = SerializerHelpers.reflectAllFields(cfg.getClass(), cfg);
    /// for (var entry : map.entrySet()) {
    ///     System.out.println(entry.getKey().getName() + " = " + entry.getValue());
    /// }
    /// </code>
    /// </example>
    public static LinkedHashMap<Field,Object> reflectAllFields(Class<?> parentClass,Object instance) throws IllegalAccessException {
        var map = new LinkedHashMap<Field,Object>();
        for (var field : parentClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(DontSave.class)) continue;
            var mods = field.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isPrivate(mods) || Modifier.isProtected(mods)) continue;

            field.setAccessible(true);
            var value = field.get(instance);
            if (value == null && !field.isAnnotationPresent(DontDefault.class)) {
                var test = DreamVariableTestAPI.returnTestFromType(field.getType());
                if (field.getType() == Date.class) value = new Date();
                else if (test != null) value = test.ReturnDefaultValue();
                else value = createBlank(field.getType());
                if (value != null) field.set(instance, value);
            }
            if (value != null) map.put(field, value);
        }
        if (instance instanceof StaticEnumPulseConfig) {
            var sup = parentClass.getSuperclass();
            if (sup != null && StaticEnumPulseConfig.class.isAssignableFrom(sup)) {
                var parent = reflectAllFields(sup, instance);
                parent.forEach(map::putIfAbsent);
            }
        }
        return map;
    }

    /// <summary>
    /// Creates an instance using a string-ID constructor if available, otherwise falls back to default constructor.
    /// </summary>
    /// <param name="id">String identifier to pass to the constructor.</param>
    /// <param name="type">Class type to instantiate.</param>
    /// <returns>A new instance or <c>null</c> if construction fails.</returns>
    /// <example>
    /// <code>
    /// Object obj = SerializerHelpers.createInstanceWithId("myId", MyType.class);
    /// </code>
    /// </example>
    public static Object createInstanceWithId(String id, Class<?> type) {
        try { return type.getConstructor(String.class).newInstance(id); }
        catch (InvocationTargetException|InstantiationException|IllegalAccessException|NoSuchMethodException ignored) {
            try { return type.getConstructor().newInstance(); }
            catch (InvocationTargetException|InstantiationException|IllegalAccessException|NoSuchMethodException e) { return null; }
        }
    }

    /// <summary>
    /// Creates an instance using the default constructor.
    /// </summary>
    /// <param name="type">Class type to instantiate.</param>
    /// <returns>A new instance or <c>null</c> if construction fails.</returns>
    /// <example>
    /// <code>
    /// Object blank = SerializerHelpers.createBlank(MyType.class);
    /// </code>
    /// </example>
    public static Object createBlank(Class<?> type) {
        try { return type.getConstructor().newInstance(); }
        catch (InvocationTargetException|InstantiationException|IllegalAccessException|NoSuchMethodException ignored) { return null; }
    }

    /// <summary>
    /// Resolves the persisted name for a field.
    /// </summary>
    /// <param name="f">Field to inspect.</param>
    /// <returns>
    /// The value of <see cref="SaveName"/> if present and non-blank,
    /// otherwise the Java field name.
    /// </returns>
    /// <example>
    /// <code>
    /// String saveName = SerializerHelpers.resolveSaveName(field);
    /// </code>
    /// </example>
    public static String resolveSaveName(Field f) {
        var a = f.getAnnotation(SaveName.class);
        return (a != null && !a.value().isBlank()) ? a.value() : f.getName();
    }
}