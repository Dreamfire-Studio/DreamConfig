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

import com.dreamfirestudios.dreamconfig.Enum.StorageType;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IPulseClass;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.SaveName;
import com.dreamfirestudios.dreamconfig.Model.Serialization.CustomSerialize;
import com.dreamfirestudios.dreamconfig.Model.Serialization.DeserializationStrategy;
import com.dreamfirestudios.dreamconfig.Saveable.ICustomVariable;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableArrayList;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableHashmap;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;
import org.bson.Document;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;

/// <summary>
/// Standalone Mongo deserializer that populates an <see cref="IDreamConfig"/> instance
/// directly from a BSON <see cref="Document"/>, without delegating to the YAML deserializer.
/// </summary>
/// <remarks>
/// Supports:
/// - Types annotated with <see cref="CustomSerialize"/> via their <see cref="DeserializationStrategy"/>.
/// - Nested <see cref="IPulseClass"/> objects with <c>BeforeLoadConfig</c>/<c>AfterLoadConfig</c>.
/// - Saveable collections: <see cref="SaveableHashmap"/>, <see cref="SaveableLinkedHashMap"/>, <see cref="SaveableArrayList"/>.
/// - <see cref="ICustomVariable"/> with <c>BeforeLoad</c>/<c>AfterLoad</c>.
/// - <see cref="Date"/> via <see cref="SerializerHelpers#SIMPLE_DATE_FORMAT"/>.
/// - Primitive adapters via <see cref="DreamVariableTestAPI"/>.
/// </remarks>
/// <example>
/// <code>
/// Document top = mongoCollection.find(eq("_id", id)).first();
/// MyConfig cfg = new MyConfig(); // implements IDreamConfig
/// MongoConfigDeSerializer.read(cfg, top); // if top contains { body: {...} }
/// </code>
/// </example>
public final class MongoConfigDeSerializer {
    private MongoConfigDeSerializer() {}

    /// <summary>
    /// Populate a config from a top-level document containing a <c>body</c> field.
    /// </summary>
    /// <param name="cfg">Target config instance (will be mutated).</param>
    /// <param name="top">MongoDB document that includes a <c>body</c> sub-document.</param>
    /// <remarks>
    /// If <paramref name="top"/> or its <c>body</c> is null, the call is a no-op.
    /// </remarks>
    /// <example>
    /// <code>
    /// MongoConfigDeSerializer.read(cfg, topDoc);
    /// </code>
    /// </example>
    public static void read(IDreamConfig cfg, Document top) throws Exception {
        if (top == null) return;
        final Document body = top.get("body", Document.class);
        if (body == null) return;
        final HashMap<Object, Object> map = fromBson(body);
        populateObject(map, cfg.getClass(), cfg);
    }

    /// <summary>
    /// Populate a config from a body-only BSON document.
    /// </summary>
    /// <param name="cfg">Target config instance (will be mutated).</param>
    /// <param name="body">The body document containing serialized fields.</param>
    /// <remarks>
    /// If <paramref name="body"/> is null, the call is a no-op.
    /// </remarks>
    /// <example>
    /// <code>
    /// MongoConfigDeSerializer.readBody(cfg, bodyDoc);
    /// </code>
    /// </example>
    public static void readBody(IDreamConfig cfg, Document body) throws Exception {
        if (body == null) return;
        final HashMap<Object, Object> map = fromBson(body);
        populateObject(map, cfg.getClass(), cfg);
    }

    /// <summary>
    /// Convert a BSON <see cref="Document"/> recursively into a <see cref="HashMap{Object,Object}"/>.
    /// </summary>
    /// <param name="d">Source BSON document.</param>
    /// <returns>Plain HashMap suitable for downstream deserialization.</returns>
    /// <example>
    /// <code>
    /// HashMap&lt;Object,Object&gt; map = MongoConfigDeSerializer.fromBson(doc);
    /// </code>
    /// </example>
    public static HashMap<Object, Object> fromBson(Document d) {
        final HashMap<Object, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> e : d.entrySet()) out.put(e.getKey(), fromBsonValue(e.getValue()));
        return out;
    }

    /// <summary>
    /// Convert a single BSON value to plain Java types recursively.
    /// </summary>
    /// <param name="v">Value from a BSON document.</param>
    /// <returns>Converted value (Document→Map, List→ArrayList, primitives passthrough).</returns>
    /// <example>
    /// <code>
    /// Object converted = MongoConfigDeSerializer.fromBsonValue(raw);
    /// </code>
    /// </example>
    public static Object fromBsonValue(Object v) {
        if (v instanceof Document sub) return fromBson(sub);
        if (v instanceof List<?> list) {
            final ArrayList<Object> out = new ArrayList<>(list.size());
            for (Object item : list) out.add(fromBsonValue(item));
            return out;
        }
        return v;
    }

    /// <summary>
    /// Populate all fields on <paramref name="target"/> from <paramref name="configData"/>.
    /// </summary>
    /// <param name="configData">Key/value pairs representing serialized fields.</param>
    /// <param name="parentClass">Declaring class of the target instance.</param>
    /// <param name="target">Object instance to mutate.</param>
    /// <returns>The mutated <paramref name="target"/> for convenience.</returns>
    /// <remarks>
    /// Field names respect <see cref="SaveName"/> if present; otherwise the Java field name is used.
    /// Unknown or missing fields are ignored.
    /// </remarks>
    /// <example>
    /// <code>
    /// MongoConfigDeSerializer.populateObject(map, cfg.getClass(), cfg);
    /// </code>
    /// </example>
    public static Object populateObject(HashMap<Object, Object> configData, Class<?> parentClass, Object target) throws Exception {
        final LinkedHashMap<Field, Object> currentFields = SerializerHelpers.reflectAllFields(parentClass, target);
        for (Field f : currentFields.keySet()) {
            String name = f.isAnnotationPresent(SaveName.class) ? f.getAnnotation(SaveName.class).value() : f.getName();
            if (name.isEmpty()) name = f.getName();
            if (!configData.containsKey(name)) continue;

            final Object storedValue = configData.get(name);
            final Object deserialized = loadSingle(f.getType(), currentFields.get(f), storedValue);
            try {
                f.set(target, deserialized);
            } catch (Exception ignored) {
                f.set(target, currentFields.get(f));
            }
        }
        return target;
    }

    /// <summary>
    /// Deserialize a single field value for a given target type.
    /// </summary>
    /// <param name="targetType">The destination Java type.</param>
    /// <param name="currentValue">The current field value on the target instance (may be used as a receiver).</param>
    /// <param name="raw">Raw data from Mongo (Map/List/primitives).</param>
    /// <returns>The deserialized object, or <c>null</c> if <paramref name="currentValue"/> or <paramref name="raw"/> is null.</returns>
    /// <remarks>
    /// Resolution order:
    /// 1) <see cref="CustomSerialize"/> strategy on the target type.
    /// 2) <see cref="IPulseClass"/> nested object (calls <c>BeforeLoadConfig</c>/<c>AfterLoadConfig</c>).
    /// 3) Saveable collections via <c>deSerializeData</c>.
    /// 4) <see cref="ICustomVariable"/> with lifecycle hooks.
    /// 5) <see cref="Date"/> via <see cref="SerializerHelpers#SIMPLE_DATE_FORMAT"/>.
    /// 6) Primitive adapters via <see cref="DreamVariableTestAPI"/>.
    /// 7) Fallback: pass the raw value through unchanged.
    /// </remarks>
    /// <example>
    /// <code>
    /// Object fieldValue = MongoConfigDeSerializer.loadSingle(field.getType(), field.get(target), rawValue);
    /// </code>
    /// </example>
    public static Object loadSingle(Class<?> targetType, Object currentValue, Object raw) throws Exception {
        if (currentValue == null || raw == null) return null;

        if (targetType.isAnnotationPresent(CustomSerialize.class)) {
            final CustomSerialize cs = targetType.getAnnotation(CustomSerialize.class);
            final DeserializationStrategy<?> strategy = cs.deserializationStrategy().getDeclaredConstructor().newInstance();
            return strategy.deserialize(raw);
        }

        if (IPulseClass.class.isAssignableFrom(targetType)) {
            final IPulseClass pulse = (IPulseClass) currentValue;
            pulse.BeforeLoadConfig();
            final Object filled = populateObject(asMap(raw), pulse.getClass(), pulse);
            pulse.AfterLoadConfig();
            return filled;
        }

        if (SaveableHashmap.class.isAssignableFrom(targetType)) {
            @SuppressWarnings("unchecked") final SaveableHashmap<Object, Object> map = (SaveableHashmap<Object, Object>) currentValue;
            map.deSerializeData(StorageType.CONFIG, asMap(raw));
            return map;
        }

        if (SaveableLinkedHashMap.class.isAssignableFrom(targetType)) {
            @SuppressWarnings("unchecked") final SaveableLinkedHashMap<Object, Object> map = (SaveableLinkedHashMap<Object, Object>) currentValue;
            map.deSerializeData(StorageType.CONFIG, asMap(raw));
            return map;
        }

        if (SaveableArrayList.class.isAssignableFrom(targetType)) {
            @SuppressWarnings("unchecked") final SaveableArrayList<Object> list = (SaveableArrayList<Object>) currentValue;
            list.deSerializeData(StorageType.CONFIG, asList(raw));
            return list;
        }

        if (ICustomVariable.class.isAssignableFrom(targetType)) {
            final ICustomVariable custom = (ICustomVariable) targetType.getDeclaredConstructor().newInstance();
            @SuppressWarnings("unchecked") final LinkedHashMap<Object, Object> m = new LinkedHashMap<>(asMap(raw));
            custom.BeforeLoad();
            custom.DeSerializeData(m);
            custom.AfterLoad();
            return custom;
        }

        if (Date.class.isAssignableFrom(targetType)) return parseDate(raw);

        final var variableTest = DreamVariableTestAPI.returnTestFromType(targetType);
        if (variableTest != null) return variableTest.DeSerializeData(raw);

        return raw;
    }

    /// <summary>
    /// Coerce an arbitrary value to a mutable <see cref="HashMap{Object,Object}"/>.
    /// </summary>
    /// <param name="v">Expected Map-like object.</param>
    /// <returns>A new <see cref="HashMap{Object,Object}"/> containing entries of <paramref name="v"/>.</returns>
    /// <exception cref="IllegalArgumentException">Thrown if input is not Map-like.</exception>
    private static HashMap<Object, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> m) {
            final HashMap<Object, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) out.put(e.getKey(), e.getValue());
            return out;
        }
        throw new IllegalArgumentException("Expected Map-like value but got: " + v.getClass().getName());
    }

    /// <summary>
    /// Coerce an arbitrary value to a mutable <see cref="ArrayList{Object}"/>.
    /// </summary>
    /// <param name="v">Expected List-like object.</param>
    /// <returns>A new <see cref="ArrayList{Object}"/> with the list contents.</returns>
    /// <exception cref="IllegalArgumentException">Thrown if input is not List-like.</exception>
    @SuppressWarnings("unchecked")
    private static ArrayList<Object> asList(Object v) {
        if (v instanceof List<?> l) return new ArrayList<>((List<Object>) l);
        throw new IllegalArgumentException("Expected List-like value but got: " + v.getClass().getName());
    }

    /// <summary>
    /// Parse a value into a <see cref="Date"/> using standard config date handling.
    /// </summary>
    /// <param name="raw">Either a <see cref="Date"/> instance or a parseable string.</param>
    /// <returns>A <see cref="Date"/> instance.</returns>
    /// <exception cref="ParseException">If the string cannot be parsed.</exception>
    private static Date parseDate(Object raw) throws ParseException {
        if (raw instanceof Date d) return d;
        return SerializerHelpers.SIMPLE_DATE_FORMAT.parse(String.valueOf(raw));
    }
}