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
import com.dreamfirestudios.dreamconfig.Model.Interfaces.*;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamconfig.Saveable.*;
import com.dreamfirestudios.dreamconfig.Model.Serialization.CustomSerialize;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;

import java.util.*;

/// <summary>
/// Deserializes config maps into live objects (inverse of <see cref="DreamConfigSerializer"/>).
/// </summary>
/// <remarks>
/// Handles primitives, custom serializable types, collections, and nested config objects.
/// Invokes lifecycle hooks like <c>BeforeLoadConfig</c> / <c>AfterLoadConfig</c>.
/// </remarks>
public final class DreamConfigDeSerializer {
    private DreamConfigDeSerializer() {}

    /// <summary>
    /// Loads configuration data from a <see cref="DreamConfigObject"/> into a config instance.
    /// </summary>
    /// <param name="cfg">Config instance to populate.</param>
    /// <param name="obj">Repository wrapper providing stored data.</param>
    public static void load(IDreamConfig cfg, DreamConfigObject obj) throws Exception {
        cfg.BeforeLoadConfig();
        @SuppressWarnings("unchecked")
        var map = obj.asDeepMap(cfg.documentID());
        readObject(map, cfg.getClass(), cfg);
        cfg.AfterLoadConfig();
    }

    /// <summary>
    /// Reads a map into an object instance using reflection.
    /// </summary>
    /// <param name="data">Map of field values.</param>
    /// <param name="type">Target class type.</param>
    /// <param name="target">Instance to populate.</param>
    /// <returns>The updated target object.</returns>
    public static Object readObject(Map<Object,Object> data, Class<?> type, Object target) throws Exception {
        var fields = SerializerHelpers.reflectAllFields(type, target);
        for (var f : fields.keySet()) {
            var name = SerializerHelpers.resolveSaveName(f);
            if (!data.containsKey(name)) continue;
            var raw = data.get(name);
            var current = fields.get(f);
            var des = readValue(current.getClass(), current, raw);
            try { f.set(target, des); } catch (Exception ignored) { f.set(target, current); }
        }
        return target;
    }

    /// <summary>
    /// Reads and converts a raw value into the appropriate Java type.
    /// </summary>
    /// <param name="classType">Expected target type.</param>
    /// <param name="currentValue">Current field value.</param>
    /// <param name="raw">Raw deserialized data.</param>
    /// <returns>Deserialized value.</returns>
    public static Object readValue(Class<?> classType, Object currentValue, Object raw) throws Exception {
        if (currentValue == null || raw == null) return null;
        var primitive = DreamVariableTestAPI.returnTestFromType(classType);

        if (classType.isAnnotationPresent(CustomSerialize.class)) {
            var ann = classType.getAnnotation(CustomSerialize.class);
            var strategy = ann.deserializationStrategy().getDeclaredConstructor().newInstance();
            return strategy.deserialize(raw);
        }
        if (IPulseClass.class.isAssignableFrom(classType)) {
            var pulse = (IPulseClass) currentValue;
            pulse.BeforeLoadConfig();
            var res = readObject((Map<Object,Object>) raw, pulse.getClass(), pulse);
            pulse.AfterLoadConfig();
            return res;
        } else if (SaveableHashmap.class.isAssignableFrom(classType)) {
            var map = (SaveableHashmap<?,?>) currentValue;
            map.deSerializeData(StorageType.CONFIG,(Map<Object,Object>) raw);
            return map;
        } else if (SaveableLinkedHashMap.class.isAssignableFrom(classType)) {
            var map = (SaveableLinkedHashMap<?,?>) currentValue;
            map.deSerializeData(StorageType.CONFIG,(Map<Object,Object>) raw);
            return map;
        } else if (SaveableArrayList.class.isAssignableFrom(classType)) {
            var list = (SaveableArrayList<?>) currentValue;
            list.deSerializeData(StorageType.CONFIG,(List<Object>) raw);
            return list;
        } else if (ICustomVariable.class.isAssignableFrom(classType)) {
            var cv = (ICustomVariable) classType.getConstructor().newInstance();
            cv.BeforeLoad();
            cv.DeSerializeData((LinkedHashMap<Object,Object>) raw);
            cv.AfterLoad();
            return cv;
        } else if (Date.class.isAssignableFrom(classType)) {
            return SerializerHelpers.SIMPLE_DATE_FORMAT.parse(raw.toString());
        } else if (primitive != null) {
            return primitive.DeSerializeData(raw);
        }
        return raw;
    }
}