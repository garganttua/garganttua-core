package com.garganttua.core.reflection.binders;

import com.garganttua.core.reflection.ReflectionException;

public interface IContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType>
        extends IFieldBinder<OnwerType, FieldType> {

    Class<OwnerContextType> getOwnerContextType();

    Class<FieldContextType> getValueContextType();

    void setValue(OwnerContextType ownerContext, FieldContextType valueContext) throws ReflectionException;

    FieldType getValue(OwnerContextType ownerContext) throws ReflectionException;

    @Override
    default void setValue() throws ReflectionException {
        throw new ReflectionException(
                "Owner context of type " + getOwnerContextType().getSimpleName() + " required for this supplier");
    };

    @Override
    default FieldType getValue() throws ReflectionException {
        throw new ReflectionException(
                "Owner context of type " + getOwnerContextType().getSimpleName() + " required for this supplier");
    };

}
