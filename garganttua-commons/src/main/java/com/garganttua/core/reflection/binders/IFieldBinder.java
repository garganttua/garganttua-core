package com.garganttua.core.reflection.binders;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supplying.IObjectSupplier;

public interface IFieldBinder<OnwerType, FieldType> {

    void setValue(IObjectSupplier<OnwerType> ownerSupplier) throws ReflectionException;

    FieldType getValue(IObjectSupplier<OnwerType> ownerSupplier) throws ReflectionException;

}
