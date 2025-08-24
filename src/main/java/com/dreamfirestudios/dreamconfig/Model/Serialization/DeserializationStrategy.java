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
/// Strategy for deserializing raw values into typed objects.
/// </summary>
/// <typeparam name="T">Target type.</typeparam>
/// <remarks>
/// Implementations are used via <see cref="CustomSerialize"/>.
/// </remarks>
/// <example>
/// <code>
/// public class MyDeserializer implements DeserializationStrategy&lt;MyType&gt; {
///     public MyType deserialize(Object raw) { return new MyType(raw.toString()); }
/// }
/// </code>
/// </example>
public interface DeserializationStrategy<T> {
    /// <summary>Convert raw deserialized value to type <typeparamref name="T"/>.</summary>
    /// <param name="rawValue">Raw value (e.g., Map, String).</param>
    /// <returns>Typed object.</returns>
    T deserialize(Object rawValue) throws Exception;
}