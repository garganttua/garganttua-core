package com.garganttua.core.dsl;

import com.garganttua.core.utils.OrderedMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderedMapBuilder<K, V extends IBuilder<B>, B> extends OrderedMap<K, V> implements IBuilder<OrderedMap<K, B>> {

    @Override
    public OrderedMap<K, B> build() throws DslException {
        log.atTrace().log("Entering build() method");
        log.atDebug().log("Building OrderedMap from {} entries", this.size());

        OrderedMap<K, B> result = this.entrySet().stream()
        .filter(e -> e.getValue() != null)
        .collect(
            OrderedMap::new,
            (map, e) -> map.put(e.getKey(), e.getValue().build()),
            (m1, m2) -> m2.forEach(m1::put)
        );

        log.atInfo().log("OrderedMap build complete with {} entries", result.size());
        log.atTrace().log("Exiting build() method");
        return result;
    }

}
