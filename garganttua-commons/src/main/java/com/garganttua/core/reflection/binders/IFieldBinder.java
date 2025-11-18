package com.garganttua.core.reflection.binders;

import com.garganttua.core.reflection.ReflectionException;

public interface IFieldBinder<OnwerType, FieldType> {

    void setValue() throws ReflectionException;

    FieldType getValue() throws ReflectionException;

    String getFieldReference();

}
