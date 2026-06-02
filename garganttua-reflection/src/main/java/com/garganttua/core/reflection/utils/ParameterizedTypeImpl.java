package com.garganttua.core.reflection.utils;

import com.garganttua.core.observability.Logger;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Synthetic {@link ParameterizedType} used to construct generic type instances at runtime
 * (e.g. {@code List<String>}) when no reflectable declaration exists.
 */
public class ParameterizedTypeImpl implements ParameterizedType {
    private static final Logger log = Logger.getLogger(ParameterizedTypeImpl.class);

    private final Type rawType;
    private final Type[] typeArguments;
    private final Type ownerType;

    /**
     * Creates a parameterized type with no owner type.
     *
     * @param rawType       the raw (erased) type
     * @param typeArguments the actual type arguments
     */
    public ParameterizedTypeImpl(Type rawType, Type[] typeArguments) {
        this(rawType, typeArguments, null);
    }

    /**
     * Creates a parameterized type.
     *
     * @param rawType       the raw (erased) type
     * @param typeArguments the actual type arguments
     * @param ownerType     the enclosing type for a nested type, or {@code null}
     */
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