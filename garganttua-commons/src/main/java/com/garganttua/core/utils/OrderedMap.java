package com.garganttua.core.utils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.runtime.Position;

/**
 * A map implementation that maintains insertion order and supports positional insertion.
 *
 * <p>
 * {@code OrderedMap} extends the standard {@link Map} interface with additional methods
 * for inserting elements at specific positions relative to existing keys. It maintains
 * the insertion order of elements and prevents duplicate keys.
 * </p>
 *
 * <p>
 * <strong>Usage Examples:</strong>
 * </p>
 * <pre>{@code
 * OrderedMap<String, String> map = new OrderedMap<>();
 *
 * // Standard insertion (appends at the end)
 * map.put("first", "First Value");
 * map.put("second", "Second Value");
 * map.put("third", "Third Value");
 *
 * // Insert before an existing key
 * map.putAt("beforeSecond", "Before Second", "second", Position.BEFORE);
 * // Order: first, beforeSecond, second, third
 *
 * // Insert after an existing key
 * map.putAt("afterFirst", "After First", "first", Position.AFTER);
 * // Order: first, afterFirst, beforeSecond, second, third
 *
 * // Using OrderedMapPosition for convenience
 * OrderedMapPosition<String> pos = OrderedMapPosition.at("third", Position.BEFORE);
 * map.putAt("beforeThird", "Before Third", pos);
 *
 * // Attempting to add duplicate key throws exception
 * try {
 *     map.put("first", "Duplicate");
 * } catch (IllegalArgumentException e) {
 *     System.out.println("Cannot add duplicate key");
 * }
 * }</pre>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 *   <li>Maintains insertion order of elements</li>
 *   <li>Prevents null keys and values</li>
 *   <li>Prevents duplicate keys</li>
 *   <li>Supports positional insertion relative to existing keys</li>
 *   <li>Implements the full {@link Map} interface</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @since 2.0.0-ALPHA01
 * @see OrderedMapPosition
 * @see Position
 */
public class OrderedMap<K, V> implements Map<K, V> {

    private final Map<K, V> internalMap = new LinkedHashMap<>();

    /**
     * Creates a new empty OrderedMap with default initial capacity.
     */
    public OrderedMap() {
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * <p>
     * This method extends the standard {@link Map#put(Object, Object)} with additional
     * validation to prevent duplicate keys and null values. The element is appended at
     * the end of the map maintaining insertion order.
     * </p>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return {@code null} (as specified by {@link Map#put})
     * @throws NullPointerException if the key or value is null
     * @throws IllegalArgumentException if the key already exists in the map
     */
    public V put(K key, V value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        if (internalMap.containsKey(key)) {
            throw new IllegalArgumentException("Key already exists: " + key);
        }

        return internalMap.put(key, value);
    }

    /**
     * Associates the specified value with the specified key at a given position.
     *
     * <p>
     * This convenience method accepts an {@link OrderedMapPosition} that encapsulates
     * both the reference key and position.
     * </p>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param position the position specification containing reference key and position
     * @return the value that was inserted
     * @throws NullPointerException if the key, value, or position is null
     * @throws IllegalArgumentException if the key already exists in the map
     * @see #putAt(Object, Object, Object, Position)
     */
    public V putAt(K key, V value, OrderedMapPosition<K> position) {
        return putAt(key, value, position.key(), position.position());
    }

    /**
     * Associates the specified value with the specified key at a position relative to a reference key.
     *
     * <p>
     * This method inserts a new key-value pair either before or after the specified reference key.
     * If the reference key is not found, the new entry is appended at the end of the map.
     * </p>
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param referenceKey the key relative to which the new entry should be positioned
     * @param position whether to insert {@link Position#BEFORE} or {@link Position#AFTER} the reference key
     * @return the value that was inserted
     * @throws NullPointerException if the key, value, or position is null
     * @throws IllegalArgumentException if the key already exists in the map
     */
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

    /**
     * Returns the value to which the specified key is mapped.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if not found
     */
    @Override
    public V get(Object key) {
        return internalMap.get(key);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     */
    @Override
    public boolean containsKey(Object key) {
        return internalMap.containsKey(key);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return internalMap.size();
    }

    /**
     * Returns a copy of this ordered map as a standard {@link LinkedHashMap}.
     *
     * <p>
     * The returned map maintains the same insertion order as this OrderedMap.
     * Modifications to the returned map do not affect this OrderedMap.
     * </p>
     *
     * @return a new LinkedHashMap containing all mappings from this map
     */
    public Map<K, V> asMap() {
        return new LinkedHashMap<>(internalMap);
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        internalMap.clear();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * @param value the value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     */
    @Override
    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with key, or {@code null} if there was no mapping
     */
    @Override
    public V remove(Object key) {
        return internalMap.remove(key);
    }

    /**
     * Copies all mappings from the specified map to this map.
     *
     * <p>
     * Each entry is added using the {@link #put(Object, Object)} method, which means
     * that duplicate keys will cause an {@link IllegalArgumentException}.
     * </p>
     *
     * @param m the map whose mappings are to be copied
     * @throws NullPointerException if the specified map is null, or contains null keys or values
     * @throws IllegalArgumentException if any key already exists in this map
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Objects.requireNonNull(m, "Map cannot be null");
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * <p>
     * The set maintains the insertion order of keys.
     * </p>
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<K> keySet() {
        return internalMap.keySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     *
     * <p>
     * The collection maintains the insertion order of values.
     * </p>
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public Collection<V> values() {
        return internalMap.values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     *
     * <p>
     * The set maintains the insertion order of entries.
     * </p>
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return internalMap.entrySet();
    }
}