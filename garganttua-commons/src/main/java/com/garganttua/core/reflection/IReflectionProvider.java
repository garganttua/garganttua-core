package com.garganttua.core.reflection;

public interface IReflectionProvider {

    @SuppressWarnings("java:S1452")
    default IClass<?> getClass(Object object) {
        return getClass(object.getClass());
    }

    <T> IClass<T> getClass(Class<T> clazz);

    <T> IClass<T> forName(String className) throws ClassNotFoundException;

    <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader) throws ClassNotFoundException;

    boolean supports(Class<?> type);

}