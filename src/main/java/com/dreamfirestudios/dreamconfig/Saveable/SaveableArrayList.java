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

import java.util.*;
import java.util.function.Function;

/// <summary>
/// Serializable list supporting Pulse classes and primitives.
/// </summary>
/// <typeparam name="E">Element type.</typeparam>
/// <remarks>
/// For <see cref="IPulseClass"/> elements, lifecycle hooks are invoked and nested objects are populated via the config deserializer.
/// </remarks>
public final class SaveableArrayList<E> {
    private final List<E> arrayList = new ArrayList<>();
    private final Class<?> elementType;

    /// <summary>Create a new saveable list with an explicit element type.</summary>
    /// <param name="elementType">Concrete element class.</param>
    public SaveableArrayList(Class<?> elementType) { this.elementType = elementType; }

    /// <summary>Access the backing list.</summary>
    public List<E> getArrayList() { return arrayList; }

    /// <summary>
    /// Serialize elements using the provided writer.
    /// </summary>
    /// <param name="write">Function mapping element → serialized value.</param>
    /// <returns>ArrayList of serialized items.</returns>
    public ArrayList<Object> serialize(Function<Object, Object> write) {
        var out = new ArrayList<Object>(arrayList.size());
        for (var v : arrayList) out.add(write.apply(v));
        return out;
    }

    /// <summary>
    /// Deserialize items into this list.
    /// </summary>
    /// <param name="storageType">Storage flavor (only <c>CONFIG</c> handled here).</param>
    /// <param name="raw">Raw list data (maps or primitives).</param>
    @SuppressWarnings("unchecked")
    public void deSerializeData(StorageType storageType, List<Object> raw) throws Exception {
        for (var item : raw) {
            if (IPulseClass.class.isAssignableFrom(elementType)) {
                var pulse = (IPulseClass) SerializerHelpers.createBlank(elementType);
                pulse.BeforeLoadConfig();
                Object des = null;
                if (storageType == StorageType.CONFIG)
                    des = DreamConfigDeSerializer.readObject((Map<Object, Object>) item, pulse.getClass(), pulse);
                arrayList.add((E) des);
                pulse.AfterLoadConfig();
            } else {
                if (storageType == StorageType.CONFIG)
                    arrayList.add((E) DreamConfigDeSerializer.readValue(elementType, item, item));
            }
        }
    }
}