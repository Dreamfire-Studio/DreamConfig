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
package com.dreamfirestudios.dreamconfig.Model;

import com.dreamfirestudios.dreamconfig.Saveable.SaveableLinkedHashMap;

/// <summary>
/// Static config keyed by enum values with type-safe defaults and automatic key hydration.
/// </summary>
/// <typeparam name="T">Self type.</typeparam>
/// <typeparam name="K">Enum key type.</typeparam>
/// <typeparam name="V">Value type.</typeparam>
/// <remarks>
/// All enum keys are guaranteed to exist after load/save by invoking <see cref="ensureAllKeysPresent"/>.
/// </remarks>
public abstract class StaticEnumPulseConfig<T extends StaticEnumPulseConfig<T, K, V>, K extends Enum<K>, V>
        extends StaticPulseConfig<T> {
    protected abstract Class<K> getKeyClass();
    protected abstract Class<V> getValueClass();
    protected abstract V getDefaultValueFor(K key);

    /// <summary>Persisted map of enum key to value.</summary>
    public final SaveableLinkedHashMap<K, V> values = new SaveableLinkedHashMap<>(getKeyClass(), getValueClass());

    /// <summary>Return stored value or default if missing.</summary>
    public V get(K key) {
        var v = values.getHashMap().get(key);
        return v == null ? getDefaultValueFor(key) : v;
    }

    @Override public void FirstLoadConfig() { ensureAllKeysPresent(); }
    @Override public void BeforeSaveConfig() { ensureAllKeysPresent(); }
    @Override public void AfterLoadConfig() {
        ensureAllKeysPresent();
        SaveDreamConfig(mainClass(), c -> {}); // persist backfilled defaults
    }

    /// <summary>Ensure all enum keys are present in <see cref="values"/> with defaults.</summary>
    private void ensureAllKeysPresent() {
        for (K k : getKeyClass().getEnumConstants()) values.getHashMap().putIfAbsent(k, getDefaultValueFor(k));
    }
}