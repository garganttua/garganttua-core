package com.garganttua.core.utils;

import com.garganttua.core.runtime.Position;

/**
 * Record representing a position specification for ordered map insertion.
 *
 * <p>
 * {@code OrderedMapPosition} encapsulates a reference key and a position indicator
 * ({@link Position#BEFORE} or {@link Position#AFTER}) for use with
 * {@link OrderedMap#putAt(Object, Object, OrderedMapPosition)} operations.
 * </p>
 *
 * <p>
 * <strong>Usage Examples:</strong>
 * </p>
 * <pre>{@code
 * OrderedMap<String, String> map = new OrderedMap<>();
 * map.put("first", "First Value");
 * map.put("second", "Second Value");
 *
 * // Create position specification
 * OrderedMapPosition<String> beforeSecond = OrderedMapPosition.at("second", Position.BEFORE);
 *
 * // Use position to insert new entry
 * map.putAt("middle", "Middle Value", beforeSecond);
 * // Order: first, middle, second
 *
 * // Or inline
 * map.putAt("last", "Last Value", OrderedMapPosition.at("second", Position.AFTER));
 * // Order: first, middle, second, last
 * }</pre>
 *
 * @param <K> the type of the reference key
 * @param key the reference key relative to which insertion should occur
 * @param position whether to insert {@link Position#BEFORE} or {@link Position#AFTER} the reference key
 *
 * @since 2.0.0-ALPHA01
 * @see OrderedMap
 * @see Position
 */
public record OrderedMapPosition<K>(K key, Position position) {

    /**
     * Factory method to create an OrderedMapPosition instance.
     *
     * <p>
     * This static factory method provides a more readable way to create position
     * specifications compared to the record constructor.
     * </p>
     *
     * @param <K> the type of the reference key
     * @param key the reference key relative to which insertion should occur
     * @param p the position indicator (BEFORE or AFTER)
     * @return a new OrderedMapPosition instance
     */
    public static <K> OrderedMapPosition<K> at(K key, Position p){
        return new OrderedMapPosition<>(key, p);
    }

}
