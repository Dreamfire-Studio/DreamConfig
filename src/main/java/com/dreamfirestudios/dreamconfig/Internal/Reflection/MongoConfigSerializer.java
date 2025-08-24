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

import com.dreamfirestudios.dreamconfig.Model.Interfaces.DontDefault;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.DontSave;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.SaveName;
import com.dreamfirestudios.dreamconfig.Model.StaticEnumPulseConfig;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;
import org.bson.Document;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.*;

/// <summary>
/// Serializes an <see cref="IDreamConfig"/> into a MongoDB-friendly <see cref="Document"/>.
/// </summary>
/// <remarks>
/// Field collection is reflection-based and respects:
/// - <see cref="DontSave"/> to skip fields,
/// - visibility: skips <c>static</c>, <c>private</c>, and <c>protected</c> fields,
/// - <see cref="SaveName"/> to rename persisted fields,
/// - <see cref="DontDefault"/> to avoid generating defaults when <c>null</c>.
/// Defaults are produced via:
/// - <see cref="Date"/> → <c>new Date()</c>,
/// - <see cref="DreamVariableTestAPI"/> adapter <c>ReturnDefaultValue()</c>,
/// - zero-arg constructor when available.
/// Date values are formatted with <c>yyyy-MMM-dd-HH-mm-ss</c> (Locale.ENGLISH).
/// When the object is a <see cref="StaticEnumPulseConfig"/>, superclass fields of that type are included as well.
/// </remarks>
/// <example>
/// <code>
/// IDreamConfig cfg = new MyConfig();
/// Document root = MongoConfigSerializer.write(cfg);
/// collection.replaceOne(eq("_id", cfg.documentID()), root, new ReplaceOptions().upsert(true));
/// </code>
/// </example>
public final class MongoConfigSerializer {

    /// <summary>Fixed-format date formatter used for stable string output.</summary>
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MMM-dd-HH-mm-ss", Locale.ENGLISH);

    private MongoConfigSerializer() {}

    /// <summary>
    /// Build a full Mongo <see cref="Document"/> with <c>_id</c> and <c>body</c>.
    /// </summary>
    /// <param name="cfg">Config instance to serialize.</param>
    /// <returns>Root document containing <c>_id</c> and serialized <c>body</c>.</returns>
    /// <example>
    /// <code>
    /// Document root = MongoConfigSerializer.write(cfg);
    /// </code>
    /// </example>
    public static Document write(IDreamConfig cfg) throws Exception {
        final Document root = new Document("_id", cfg.documentID());
        root.put("body", writeBody(cfg));
        return root;
    }

    /// <summary>
    /// Serialize only the body of a config (no <c>_id</c>).
    /// </summary>
    /// <param name="cfg">Config instance to serialize.</param>
    /// <returns><see cref="Document"/> representing the serialized body.</returns>
    /// <example>
    /// <code>
    /// Document body = MongoConfigSerializer.writeBody(cfg);
    /// </code>
    /// </example>
    public static Document writeBody(IDreamConfig cfg) throws Exception {
        final LinkedHashMap<String, Object> javaTree = extractAllFields(cfg.getClass(), cfg);
        return toBson(javaTree);
    }

    /// <summary>
    /// Reflect and extract all eligible fields (including selected superclass fields), producing a deterministic map.
    /// </summary>
    /// <param name="parentClass">The class that declares the fields to inspect.</param>
    /// <param name="object">The instance to read field values from.</param>
    /// <returns>Ordered map of persisted field names to their Java values.</returns>
    /// <remarks>
    /// - Skips fields marked <see cref="DontSave"/> or with disallowed modifiers.<br/>
    /// - Generates defaults unless <see cref="DontDefault"/> is present and the value is <c>null</c>.<br/>
    /// - When <paramref name="object"/> is a <see cref="StaticEnumPulseConfig"/>, recursively extracts superclass fields of that type.
    /// </remarks>
    /// <example>
    /// <code>
    /// LinkedHashMap&lt;String,Object&gt; data = extractAllFields(cfg.getClass(), cfg);
    /// </code>
    /// </example>
    private static LinkedHashMap<String, Object> extractAllFields(Class<?> parentClass, Object object) throws IllegalAccessException {
        final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

        for (Field field : parentClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(DontSave.class)) continue;
            final int mods = field.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isPrivate(mods) || Modifier.isProtected(mods)) continue;

            field.setAccessible(true);
            Object value = field.get(object);

            if (value == null && !field.isAnnotationPresent(DontDefault.class)) {
                final var variableTest = DreamVariableTestAPI.returnTestFromType(field.getType());
                if (field.getType() == Date.class) {
                    value = new Date();
                } else if (variableTest != null) {
                    value = variableTest.ReturnDefaultValue();
                } else {
                    try {
                        value = field.getType().getConstructor().newInstance();
                    } catch (Exception ignored) { /* fall through: leave null */ }
                }
            }

            if (value != null) {
                final String name = field.isAnnotationPresent(SaveName.class)
                        ? field.getAnnotation(SaveName.class).value()
                        : field.getName();
                data.put(name, value);
            }
        }

        // Include StaticEnumPulseConfig superclass fields (if applicable)
        if (object instanceof StaticEnumPulseConfig) {
            final Class<?> superClass = parentClass.getSuperclass();
            if (superClass != null && StaticEnumPulseConfig.class.isAssignableFrom(superClass)) {
                data.putAll(extractAllFields(superClass, object));
            }
        }
        return data;
    }

    /// <summary>
    /// Convert a Java map tree into a BSON <see cref="Document"/> recursively.
    /// </summary>
    /// <param name="map">Map of keys to values (which may be nested maps/lists).</param>
    /// <returns>BSON document containing converted values.</returns>
    /// <example>
    /// <code>
    /// Document doc = MongoConfigSerializer.toBson(map);
    /// </code>
    /// </example>
    public static Document toBson(Map<String, Object> map) {
        final Document d = new Document();
        for (Map.Entry<String, Object> e : map.entrySet()) d.put(e.getKey(), toBsonValue(e.getValue()));
        return d;
    }

    /// <summary>
    /// Convert a single Java value to a BSON-storable value recursively.
    /// </summary>
    /// <param name="v">Value to convert.</param>
    /// <returns>
    /// - Map → <see cref="Document"/> (keys coerced to String), values converted recursively;<br/>
    /// - List → <see cref="List"/> with items converted recursively;<br/>
    /// - <see cref="Date"/> → formatted <see cref="String"/>;<br/>
    /// - Otherwise: passthrough.
    /// </returns>
    /// <example>
    /// <code>
    /// Object out = MongoConfigSerializer.toBsonValue(value);
    /// </code>
    /// </example>
    public static Object toBsonValue(Object v) {
        if (v == null) return null;

        if (v instanceof Map<?, ?> m) {
            final Map<String, Object> cast = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet())
                cast.put(String.valueOf(e.getKey()), toBsonValue(e.getValue()));
            return toBson(cast);
        }

        if (v instanceof List<?> list) {
            final List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) out.add(toBsonValue(item));
            return out;
        }

        if (v instanceof Date date) return SIMPLE_DATE_FORMAT.format(date);

        return v;
    }
}