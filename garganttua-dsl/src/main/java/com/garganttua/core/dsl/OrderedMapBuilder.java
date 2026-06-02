package com.garganttua.core.dsl;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.utils.OrderedMap;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class OrderedMapBuilder<K, V extends IBuilder<B>, B> extends OrderedMap<K, V> implements IBuilder<OrderedMap<K, B>> {
    private static final Logger log = Logger.getLogger(OrderedMapBuilder.class);

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
