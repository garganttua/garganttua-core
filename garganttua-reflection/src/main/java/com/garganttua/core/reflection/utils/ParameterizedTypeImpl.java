package com.garganttua.core.reflection.utils;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ParameterizedTypeImpl implements ParameterizedType {
    private static final IDiagnostic log = Diagnostics.of(ParameterizedTypeImpl.class);

    private final Type rawType;
    private final Type[] typeArguments;
    private final Type ownerType;

    public ParameterizedTypeImpl(Type rawType, Type[] typeArguments) {
        this(rawType, typeArguments, null);
    }

    public ParameterizedTypeImpl(Type rawType, Type[] typeArguments, Type ownerType) {
        log.trace("Creating ParameterizedTypeImpl: rawType={}, typeArguments={}, ownerType={}", rawType, typeArguments, ownerType);
        this.rawType = rawType;
        this.typeArguments = typeArguments;
        this.ownerType = ownerType;
        log.debug("Created ParameterizedType: {}", this);
    }

    @Override
    public Type[] getActualTypeArguments() {
        log.trace("Getting actual type arguments for {}", rawType);
        return typeArguments;
    }

    @Override
    public Type getRawType() {
        log.trace("Getting raw type: {}", rawType);
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        log.trace("Getting owner type: {}", ownerType);
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