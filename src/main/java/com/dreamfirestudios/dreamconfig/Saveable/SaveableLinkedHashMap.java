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
package com.dreamfirestudios.dreamconfig.Saveable;

import com.dreamfirestudios.dreamconfig.Enum.StorageType;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IPulseClass;
import com.dreamfirestudios.dreamconfig.Internal.Reflection.SerializerHelpers;
import com.dreamfirestudios.dreamconfig.Internal.Reflection.DreamConfigDeSerializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/// <summary>
/// Serializable LinkedHashMap supporting Pulse classes and primitives.
/// </summary>
/// <typeparam name="K">Key type.</typeparam>
/// <typeparam name="V">Value type.</typeparam>
/// <remarks>
/// Maintains insertion order on serialization. Handles nested <see cref="IPulseClass"/> keys/values.
/// </remarks>
public final class SaveableLinkedHashMap<K, V> {
    private final LinkedHashMap<K, V> map = new LinkedHashMap<>();
    private final Class<?> keyType;
    private final Class<?> valueType;

    /// <summary>Create a new saveable linked hash map with explicit key and value types.</summary>
    public SaveableLinkedHashMap(Class<?> keyType, Class<?> valueType) { this.keyType = keyType; this.valueType = valueType; }

    /// <summary>Access the backing ordered map.</summary>
    public LinkedHashMap<K, V> getHashMap() { return map; }

    /// <summary>
    /// Serialize entries using a provided writer.
    /// </summary>
    /// <param name="write">Function mapping objects → serialized form.</param>
    /// <returns>LinkedHashMap of serialized key/value pairs.</returns>
    public LinkedHashMap<Object, Object> serialize(Function<Object, Object> write) {
        var out = new LinkedHashMap<Object, Object>(map.size());
        for (var k : map.keySet()) out.put(write.apply(k), write.apply(map.get(k)));
        return out;
    }

    /// <summary>
    /// Deserialize entries into this map.
    /// </summary>
    /// <param name="storage">Storage flavor (only <c>CONFIG</c> handled here).</param>
    /// <param name="raw">Raw map data.</param>
    @SuppressWarnings("unchecked")
    public void deSerializeData(StorageType storage, Map<Object, Object> raw) throws Exception {
        for (var k : raw.keySet()) {
            var dk = deKey(storage, k, keyType);
            var dv = deVal(storage, raw.get(k), valueType);
            map.put(dk, dv);
        }
    }

    @SuppressWarnings("unchecked")
    private K deKey(StorageType storage, Object source, Class<?> type) throws Exception {
        if (IPulseClass.class.isAssignableFrom(type)) {
            var pulse = (IPulseClass) SerializerHelpers.createBlank(type);
            pulse.BeforeLoadConfig();
            Object des = DreamConfigDeSerializer.readObject((Map<Object, Object>) source, pulse.getClass(), pulse);
            pulse.AfterLoadConfig();
            return (K) des;
        } else return (K) DreamConfigDeSerializer.readValue(type, source, source);
    }

    @SuppressWarnings("unchecked")
    private V deVal(StorageType storage, Object source, Class<?> type) throws Exception {
        if (IPulseClass.class.isAssignableFrom(type)) {
            var pulse = (IPulseClass) SerializerHelpers.createBlank(type);
            pulse.BeforeLoadConfig();
            Object des = DreamConfigDeSerializer.readObject((Map<Object, Object>) source, pulse.getClass(), pulse);
            pulse.AfterLoadConfig();
            return (V) des;
        } else return (V) DreamConfigDeSerializer.readValue(type, source, source);
    }
}