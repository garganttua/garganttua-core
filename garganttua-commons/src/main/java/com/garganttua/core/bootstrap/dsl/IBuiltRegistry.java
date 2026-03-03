package com.garganttua.core.bootstrap.dsl;

import java.util.List;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

public interface IBuiltRegistry {

    <T>Optional<T> request(IClass<T> clazz);

    Integer size();

    List<Object> toList();

}
