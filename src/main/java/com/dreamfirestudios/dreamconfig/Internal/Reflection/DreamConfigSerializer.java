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

import com.dreamfirestudios.dreamconfig.Model.Interfaces.DreamConfigFooter;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.DreamConfigHeader;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IPulseClass;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.StorageComment;
import com.dreamfirestudios.dreamconfig.Model.Serialization.CustomSerialize;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamconfig.Saveable.ICustomVariable;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableArrayList;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableHashmap;
import com.dreamfirestudios.dreamconfig.Saveable.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Date;
import java.util.LinkedHashMap;

/// <summary>
/// Serializes DreamConfig objects to a map structure suitable for repositories (YAML, Mongo, etc.).
/// </summary>
/// <remarks>
/// Handles:
/// - Primitives via <see cref="DreamVariableTestAPI"/> adapters,
/// - Custom serializable types marked with <see cref="CustomSerialize"/>,
/// - Saveable collections (<see cref="SaveableHashmap"/>, <see cref="SaveableLinkedHashMap"/>, <see cref="SaveableArrayList"/>),
/// - Bukkit <see cref="ConfigurationSerializable"/> objects,
/// - Nested Pulse objects (<see cref="IPulseClass"/>),
/// - <see cref="Date"/> (formatted via <see cref="SerializerHelpers#SIMPLE_DATE_FORMAT"/>).
/// Invokes lifecycle hooks like <c>BeforeSaveConfig</c>/<c>AfterSaveConfig</c> and <c>BeforeSave</c>/<c>AfterSave</c>.
/// </remarks>
/// <example>
/// <code>
/// var cfg = new MyConfig(); // implements IDreamConfig
/// var repoObj = new DreamConfigObject(cfg.documentID());
/// DreamConfigSerializer.save(cfg, repoObj); // persists with headers/footers if present
/// </code>
/// </example>
public final class DreamConfigSerializer {
    private DreamConfigSerializer() {}

    /// <summary>
    /// Saves a config instance into the repository-backed <see cref="DreamConfigObject"/>.
    /// </summary>
    /// <param name="cfg">Config instance implementing <see cref="IDreamConfig"/>.</param>
    /// <param name="obj">Repository wrapper receiving the serialized map.</param>
    /// <remarks>
    /// Applies <see cref="DreamConfigHeader"/> and <see cref="DreamConfigFooter"/> if present on the config class.
    /// Calls <c>cfg.BeforeSaveConfig()</c> prior to serialization and <c>obj.save()</c> after.
    /// </remarks>
    /// <example>
    /// <code>
    /// DreamConfigSerializer.save(cfg, repoObj);
    /// </code>
    /// </example>
    public static void save(IDreamConfig cfg, DreamConfigObject obj) throws Exception {
        cfg.BeforeSaveConfig();
        if (cfg.getClass().isAnnotationPresent(DreamConfigHeader.class))
            obj.setHeader(cfg.getClass().getAnnotation(DreamConfigHeader.class));
        obj.set(cfg.documentID(), writeObject(cfg.getClass(), cfg));
        if (cfg.getClass().isAnnotationPresent(DreamConfigFooter.class))
            obj.setFooter(cfg.getClass().getAnnotation(DreamConfigFooter.class));
        obj.save();
    }

    /// <summary>
    /// Reflects fields from <paramref name="type"/> and writes them to a <see cref="LinkedHashMap"/> for deterministic order.
    /// </summary>
    /// <param name="type">Declaring class of <paramref name="instance"/>.</param>
    /// <param name="instance">Object instance to serialize.</param>
    /// <returns>Map of field names to serialized values.</returns>
    /// <remarks>
    /// Field names are resolved via <see cref="SerializerHelpers#resolveSaveName(java.lang.reflect.Field)"/>.
    /// <see cref="StorageComment"/> annotations produce banner comment lines in the map.
    /// </remarks>
    /// <example>
    /// <code>
    /// var map = DreamConfigSerializer.writeObject(myClass, myInstance);
    /// </code>
    /// </example>
    public static LinkedHashMap<String, Object> writeObject(Class<?> type, Object instance) throws Exception {
        final var out = new LinkedHashMap<String, Object>();
        final var fields = SerializerHelpers.reflectAllFields(type, instance);
        for (var f : fields.keySet()) {
            final var comment = f.getAnnotation(StorageComment.class);
            if (comment != null && !comment.value().isBlank())
                out.put("# +------------------" + comment.value(), "------------------+ #");
            final var name = SerializerHelpers.resolveSaveName(f);
            out.put(name, writeValue(fields.get(f)));
        }
        return out;
    }

    /// <summary>
    /// Serializes a single value recursively to repository-friendly primitives/collections.
    /// </summary>
    /// <param name="value">Value to serialize (may be null).</param>
    /// <returns>Serialized representation or <c>null</c> for null input.</returns>
    /// <remarks>
    /// Order of handling:
    /// 1) <see cref="CustomSerialize"/> strategy on the value's concrete type,
    /// 2) <see cref="IPulseClass"/> nested objects with lifecycle hooks,
    /// 3) Saveable collections,
    /// 4) <see cref="ICustomVariable"/> with lifecycle hooks,
    /// 5) Bukkit <see cref="ConfigurationSerializable"/>,
    /// 6) <see cref="Date"/> via <see cref="SerializerHelpers#SIMPLE_DATE_FORMAT"/>,
    /// 7) Primitive adapters via <see cref="DreamVariableTestAPI#returnTestFromType(Class)"/>,
    /// 8) Fallback: return the value as-is (Strings/Numbers/Booleans/Maps/Lists are already serializable).
    /// </remarks>
    /// <example>
    /// <code>
    /// Object out = DreamConfigSerializer.writeValue(value);
    /// </code>
    /// </example>
    public static Object writeValue(Object value) throws Exception {
        if (value == null) return null;
        final var type = value.getClass();
        final var primitive = DreamVariableTestAPI.returnTestFromType(type);

        if (type.isAnnotationPresent(CustomSerialize.class)) {
            final var ann = type.getAnnotation(CustomSerialize.class);
            final var strategy = ann.serializationStrategy().getDeclaredConstructor().newInstance();
            return strategy.serialize(value);
        }

        if (value instanceof IPulseClass pc) {
            pc.BeforeSaveConfig();
            final var data = writeObject(pc.getClass(), pc);
            pc.AfterSaveConfig();
            return data;
        }

        if (value instanceof SaveableHashmap<?, ?> m) {
            return m.serialize(v -> { try { return writeValue(v); } catch (Exception e) { throw new RuntimeException(e); } });
        }
        if (value instanceof SaveableLinkedHashMap<?, ?> lm) {
            return lm.serialize(v -> { try { return writeValue(v); } catch (Exception e) { throw new RuntimeException(e); } });
        }
        if (value instanceof SaveableArrayList<?> al) {
            return al.serialize(v -> { try { return writeValue(v); } catch (Exception e) { throw new RuntimeException(e); } });
        }

        if (value instanceof ICustomVariable cv) {
            cv.BeforeSave();
            final var data = cv.SerializeData();
            cv.AfterSave();
            return data;
        }

        if (value instanceof ConfigurationSerializable serializable) {
            return serializable.serialize();
        }

        if (value instanceof Date d) {
            return SerializerHelpers.SIMPLE_DATE_FORMAT.format(d);
        }

        if (primitive != null) {
            return primitive.SerializeData(value);
        }

        return value;
    }
}