package com.garganttua.core.bootstrap.dsl;

import java.util.List;
import java.util.Optional;

public interface IBuiltRegistry {

    <T>Optional<T> request(Class<T> clazz);

    Integer size();

    List<Object> toList();

}
