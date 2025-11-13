package com.garganttua.core.utils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.runtime.Position;

public class OrderedMap<K, V> implements Map<K, V> {

    private final Map<K, V> internalMap = new LinkedHashMap<>();

    public V put(K key, V value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        if (internalMap.containsKey(key)) {
            throw new IllegalArgumentException("Key already exists: " + key);
        }

        return internalMap.put(key, value);
    }

    public V putAt(K key, V value, OrderedMapPosition<K> position) {
        return putAt(key, value, position.key(), position.position());
    }

    public V putAt(K key, V value, K referenceKey, Position position) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");

        if (internalMap.containsKey(key)) {
            throw new IllegalArgumentException("Key already exists: " + key);
        }

        Map<K, V> reordered = new LinkedHashMap<>();
        boolean inserted = false;

        for (Map.Entry<K, V> entry : internalMap.entrySet()) {
            K existingKey = entry.getKey();

            if (position == Position.BEFORE && existingKey.equals(referenceKey)) {
                reordered.put(key, value);
                inserted = true;
            }

            reordered.put(existingKey, entry.getValue());

            if (position == Position.AFTER && existingKey.equals(referenceKey)) {
                reordered.put(key, value);
                inserted = true;
            }
        }

        if (!inserted) {
            reordered.put(key, value);
        }

        internalMap.clear();
        internalMap.putAll(reordered);
        return value;
    }

    @Override
    public V get(Object key) {
        return internalMap.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return internalMap.containsKey(key);
    }

    public int size() {
        return internalMap.size();
    }

    public Map<K, V> asMap() {
        return new LinkedHashMap<>(internalMap);
    }

    public void clear() {
        internalMap.clear();
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }

    @Override
    public V remove(Object key) {
        return internalMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Objects.requireNonNull(m, "Map cannot be null");
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Set<K> keySet() {
        return internalMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return internalMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return internalMap.entrySet();
    }
}