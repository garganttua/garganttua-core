package com.garganttua.core.reflection.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ParameterizedTypeImpl implements ParameterizedType {

    private final Type rawType;
    private final Type[] typeArguments;
    private final Type ownerType;

    public ParameterizedTypeImpl(Type rawType, Type[] typeArguments) {
        this(rawType, typeArguments, null);
    }

    public ParameterizedTypeImpl(Type rawType, Type[] typeArguments, Type ownerType) {
        this.rawType = rawType;
        this.typeArguments = typeArguments;
        this.ownerType = ownerType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return typeArguments;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public String toString() {
        return rawType.getTypeName() + "<" +
                String.join(", ",
                        java.util.Arrays.stream(typeArguments)
                                .map(Type::getTypeName)
                                .toArray(String[]::new))
                + ">";
    }
}