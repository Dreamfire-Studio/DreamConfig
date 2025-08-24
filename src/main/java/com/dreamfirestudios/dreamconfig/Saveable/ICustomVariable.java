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

import java.util.LinkedHashMap;

/// <summary>
/// Custom serializable “value object”.
/// </summary>
/// <remarks>
/// Gives fine-grained control over how a complex value is persisted and restored.  
/// Lifecycle hooks can be used to validate or prepare internal state.
/// </remarks>
/// <example>
/// <code>
/// public final class Money implements ICustomVariable {
///   private int coins;
///   public LinkedHashMap&lt;Object,Object&gt; SerializeData(){ return new LinkedHashMap&lt;&gt;(Map.of("coins", coins)); }
///   public void DeSerializeData(LinkedHashMap&lt;Object,Object&gt; data){ this.coins = (int) data.get("coins"); }
/// }
/// </code>
/// </example>
public interface ICustomVariable {
    /// <summary>Serialize to a primitive map.</summary>
    LinkedHashMap<Object, Object> SerializeData();

    /// <summary>Populate from a primitive map.</summary>
    /// <param name="data">Key/value map read from storage.</param>
    void DeSerializeData(LinkedHashMap<Object, Object> data);

    /// <summary>Hook before a load operation.</summary>
    default void BeforeLoad() {}

    /// <summary>Hook after a load operation.</summary>
    default void AfterLoad() {}

    /// <summary>Hook before a save operation.</summary>
    default void BeforeSave() {}

    /// <summary>Hook after a save operation.</summary>
    default void AfterSave() {}
}