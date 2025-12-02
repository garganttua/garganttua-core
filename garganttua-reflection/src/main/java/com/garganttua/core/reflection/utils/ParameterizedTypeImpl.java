package com.garganttua.core.reflection.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParameterizedTypeImpl implements ParameterizedType {

    private final Type rawType;
    private final Type[] typeArguments;
    private final Type ownerType;

    public ParameterizedTypeImpl(Type rawType, Type[] typeArguments) {
        this(rawType, typeArguments, null);
    }

    public ParameterizedTypeImpl(Type rawType, Type[] typeArguments, Type ownerType) {
        log.atTrace().log("Creating ParameterizedTypeImpl: rawType={}, typeArguments={}, ownerType={}", rawType, typeArguments, ownerType);
        this.rawType = rawType;
        this.typeArguments = typeArguments;
        this.ownerType = ownerType;
        log.atDebug().log("Created ParameterizedType: {}", this);
    }

    @Override
    public Type[] getActualTypeArguments() {
        log.atTrace().log("Getting actual type arguments for {}", rawType);
        return typeArguments;
    }

    @Override
    public Type getRawType() {
        log.atTrace().log("Getting raw type: {}", rawType);
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        log.atTrace().log("Getting owner type: {}", ownerType);
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