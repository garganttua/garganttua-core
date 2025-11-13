package com.garganttua.core.utils;

import com.garganttua.core.runtime.Position;

public record OrderedMapPosition<K>(K key, Position position) {

    public static <K> OrderedMapPosition<K> at(K key, Position p){
        return new OrderedMapPosition<>(key, p);
    }

}
