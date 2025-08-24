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
package com.dreamfirestudios.dreamconfig.Model.Serialization;

/// <summary>
/// Strategy for serializing objects into repository-storable forms.
/// </summary>
/// <typeparam name="T">Serialized form type (commonly Map or String).</typeparam>
/// <remarks>
/// Implementations are used via <see cref="CustomSerialize"/>.
/// </remarks>
/// <example>
/// <code>
/// public class MySerializer implements SerializationStrategy&lt;Map&lt;String,Object&gt;&gt; {
///     public Map&lt;String,Object&gt; serialize(Object raw) { return Map.of("value", raw.toString()); }
/// }
/// </code>
/// </example>
public interface SerializationStrategy<T> {
    /// <summary>Serialize a raw object into a repository-storable value.</summary>
    /// <param name="rawValue">Object to serialize.</param>
    /// <returns>Serialized form.</returns>
    T serialize(Object rawValue) throws Exception;
}