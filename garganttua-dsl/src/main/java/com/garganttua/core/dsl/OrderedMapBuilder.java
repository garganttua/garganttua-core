package com.garganttua.core.dsl;

import com.garganttua.core.utils.OrderedMap;

public class OrderedMapBuilder<K, V extends IBuilder<B>, B> extends OrderedMap<K, V> implements IBuilder<OrderedMap<K, B>> {

    @Override
    public OrderedMap<K, B> build() throws DslException {
        return this.entrySet().stream()
        .filter(e -> e.getValue() != null)
        .collect(
            OrderedMap::new,
            (map, e) -> map.put(e.getKey(), e.getValue().build()),
            (m1, m2) -> m2.forEach(m1::put)
        );

    }

}
