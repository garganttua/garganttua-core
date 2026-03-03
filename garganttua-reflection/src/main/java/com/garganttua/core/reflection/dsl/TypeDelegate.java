package com.garganttua.core.reflection.dsl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.TypeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class TypeDelegate {

    private final IReflectionProvider provider;

    TypeDelegate(IReflectionProvider provider) {
        this.provider = provider;
    }

    IClass<?> extractClass(Type type) {
        if (type instanceof Class<?>) {
            return provider.getClass((Class<Object>) type);
        }
        if (type instanceof ParameterizedType) {
            return extractClass(((ParameterizedType) type).getRawType());
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = extractRawClass(componentType);
            Class<?> arrayClass = java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
            return provider.getClass((Class<Object>) arrayClass);
        }
        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? provider.getClass(Object.class) : extractClass(bounds[0]);
        }
        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return upperBounds.length == 0 ? provider.getClass(Object.class) : extractClass(upperBounds[0]);
        }
        throw new IllegalArgumentException("Cannot convert to IClass<?>: " + type);
    }

    private static Class<?> extractRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return extractRawClass(((ParameterizedType) type).getRawType());
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = extractRawClass(componentType);
            return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
        }
        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? Object.class : extractRawClass(bounds[0]);
        }
        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return upperBounds.length == 0 ? Object.class : extractRawClass(upperBounds[0]);
        }
        throw new IllegalArgumentException("Cannot extract Class<?> from Type: " + type);
    }

    boolean typeEquals(Type type1, Type type2) {
        if (type1 == type2) {
            return true;
        }
        if (type1 == null || type2 == null) {
            return false;
        }
        if (type1 instanceof ParameterizedType && type2 instanceof ParameterizedType) {
            if (equalsParameterizedType((ParameterizedType) type1, (ParameterizedType) type2)) {
                return true;
            }
        }
        if (type1 instanceof Class<?> && type2 instanceof Class<?>) {
            if (type1.equals(type2)) {
                return true;
            }
        }
        if (type1 instanceof GenericArrayType && type2 instanceof GenericArrayType) {
            if (typeEquals(((GenericArrayType) type1).getGenericComponentType(),
                    ((GenericArrayType) type2).getGenericComponentType())) {
                return true;
            }
        }
        if (type1 instanceof Class<?> && ((Class<?>) type1).isArray()
                && type2 instanceof Class<?> && ((Class<?>) type2).isArray()) {
            if (typeEquals(((Class<?>) type1).getComponentType(), ((Class<?>) type2).getComponentType())) {
                return true;
            }
        }

        Class<?> c1 = extractRawClass(type1);
        Class<?> c2 = extractRawClass(type2);
        return c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1);
    }

    private boolean equalsParameterizedType(ParameterizedType type1, ParameterizedType type2) {
        if (!typeEquals(type1.getRawType(), type2.getRawType())) {
            return false;
        }
        Type[] args1 = type1.getActualTypeArguments();
        Type[] args2 = type2.getActualTypeArguments();
        if (args1.length != args2.length) {
            return false;
        }
        for (int i = 0; i < args1.length; i++) {
            if (!typeEquals(args1[i], args2[i])) {
                return false;
            }
        }
        return true;
    }

    boolean isImplementingInterface(IClass<?> interfaceType, IClass<?> objectType) {
        for (IClass<?> iface : objectType.getInterfaces()) {
            if (iface.equals(interfaceType)) {
                return true;
            }
        }
        return false;
    }

    IClass<?>[] parameterTypes(Object[] args) {
        if (args == null) {
            return new IClass<?>[0];
        }
        IClass<?>[] types = new IClass<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null
                    ? provider.getClass(Object.class)
                    : provider.getClass(args[i].getClass());
        }
        return types;
    }

    boolean isComplexType(IClass<?> clazz) {
        return TypeUtils.isComplexType(clazz);
    }

    IClass<?> getGenericTypeArgument(IClass<?> type, int index) {
        Type genericSuperclass = type.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType pt) {
            Type[] typeArguments = pt.getActualTypeArguments();
            if (index < typeArguments.length && typeArguments[index] instanceof Class<?>) {
                return provider.getClass((Class<?>) typeArguments[index]);
            }
        }
        return null;
    }

    boolean isCollectionOrMapOrArray(IField field) {
        IClass<?> type = field.getType();
        return type.isArray()
                || provider.getClass(Collection.class).isAssignableFrom(type)
                || provider.getClass(Map.class).isAssignableFrom(type);
    }
}
