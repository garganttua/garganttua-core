package com.garganttua.core.aot.reflection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * AOT-friendly, immutable {@link ParameterizedType} reconstructed at build time
 * from a field's declared generic signature.
 *
 * <p>The AOT field source generator cannot emit a {@code ParameterizedType}
 * literal in generated source, so for a parameterized field such as
 * {@code List<String>} it emits {@code AOTParameterizedType.of(List.class,
 * String.class)} and passes the result as the {@code AOTField}'s generic type.
 * Without it, {@code AOTField.getGenericType()} falls back to the erased raw
 * type, downstream {@code instanceof ParameterizedType} checks fail (e.g.
 * {@code MappingRules.getFieldGenericType}), and the mapper drops from a direct
 * collection copy to per-element re-mapping.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public final class AOTParameterizedType implements ParameterizedType {

    private final Type rawType;
    private final Type[] actualTypeArguments;
    private final Type ownerType;

    private AOTParameterizedType(Type rawType, Type ownerType, Type[] actualTypeArguments) {
        this.rawType = Objects.requireNonNull(rawType, "rawType");
        this.ownerType = ownerType;
        this.actualTypeArguments = actualTypeArguments != null
                ? actualTypeArguments.clone() : new Type[0];
    }

    /**
     * Builds an owner-less parameterized type from a raw type and its type arguments.
     *
     * @param rawType the erased raw type (e.g. {@code List.class})
     * @param actualTypeArguments the type arguments (e.g. {@code String.class})
     * @return an immutable {@link ParameterizedType} equivalent to {@code rawType<actualTypeArguments>}
     */
    public static AOTParameterizedType of(Type rawType, Type... actualTypeArguments) {
        return new AOTParameterizedType(rawType, null, actualTypeArguments);
    }

    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments.clone();
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParameterizedType other)) {
            return false;
        }
        return Objects.equals(rawType, other.getRawType())
                && Objects.equals(ownerType, other.getOwnerType())
                && Arrays.equals(actualTypeArguments, other.getActualTypeArguments());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(actualTypeArguments)
                ^ Objects.hashCode(rawType)
                ^ Objects.hashCode(ownerType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rawType.getTypeName());
        if (actualTypeArguments.length > 0) {
            sb.append('<');
            for (int i = 0; i < actualTypeArguments.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(actualTypeArguments[i].getTypeName());
            }
            sb.append('>');
        }
        return sb.toString();
    }
}
