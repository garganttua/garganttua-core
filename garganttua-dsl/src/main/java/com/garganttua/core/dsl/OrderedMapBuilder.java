package com.garganttua.core.dsl;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.utils.OrderedMap;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * An insertion-ordered map whose values are themselves builders, building into an
 * {@link OrderedMap} of the same keys mapped to the built values.
 *
 * <p>
 * Each value builder is invoked during {@link #build()} while preserving key order;
 * {@code null} value builders are skipped.
 * </p>
 *
 * @param <K> the key type
 * @param <V> the value builder type, producing {@code B}
 * @param <B> the type of object produced by each value builder
 * @see IBuilder
 */
@Reflected
public class OrderedMapBuilder<K, V extends IBuilder<B>, B> extends OrderedMap<K, V> implements IBuilder<OrderedMap<K, B>> {
    private static final Logger log = Logger.getLogger(OrderedMapBuilder.class);

    /**
     * Builds each non-null value builder, preserving key insertion order.
     *
     * @return an {@link OrderedMap} mapping each key to its built value
     * @throws DslException if any value builder fails to build
     */
    @Override
    public OrderedMap<K, B> build() throws DslException {
        log.trace("Entering build() method");
        log.debug("Building OrderedMap from {} entries", this.size());

        OrderedMap<K, B> result = this.entrySet().stream()
        .filter(e -> e.getValue() != null)
        .collect(
            OrderedMap::new,
            (map, e) -> map.put(e.getKey(), e.getValue().build()),
            (m1, m2) -> m2.forEach(m1::put)
        );

        log.debug("OrderedMap build complete with {} entries", result.size());
        log.trace("Exiting build() method");
        return result;
    }

}
