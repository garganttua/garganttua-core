package com.garganttua.core.reflection;

/**
 * Pluggable provider of {@link IClass} descriptors for the reflection facade.
 *
 * <p>
 * Implementations resolve raw {@link Class} objects or class names into the
 * {@code I*} abstraction interfaces, allowing the framework to be backed either
 * by JVM runtime reflection or by AOT-generated descriptors. Multiple providers
 * may coexist and are prioritized; {@link #supports(Class)} lets the facade pick
 * the most capable provider for a given type.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IReflection
 */
public interface IReflectionProvider {

    /**
     * Resolves the {@link IClass} descriptor for the runtime class of the given object.
     *
     * @param object the object whose class to describe
     * @return the descriptor for {@code object.getClass()}
     */
    @SuppressWarnings("java:S1452")
    default IClass<?> getClass(Object object) {
        return getClass(object.getClass());
    }

    /**
     * Resolves the {@link IClass} descriptor for a raw {@link Class}.
     *
     * @param <T>   the class type
     * @param clazz the class to describe
     * @return the matching descriptor
     */
    <T> IClass<T> getClass(Class<T> clazz);

    /**
     * Resolves a class by fully qualified name.
     *
     * @param <T>       the resolved class type
     * @param className the fully qualified class name
     * @return the matching descriptor
     * @throws ClassNotFoundException if the class cannot be located
     */
    <T> IClass<T> forName(String className) throws ClassNotFoundException;

    /**
     * Resolves a class by name, controlling initialization and class loader.
     *
     * @param <T>        the resolved class type
     * @param className  the fully qualified class name
     * @param initialize whether to initialize the class
     * @param loader     the class loader to use
     * @return the matching descriptor
     * @throws ClassNotFoundException if the class cannot be located
     */
    <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader) throws ClassNotFoundException;

    /**
     * Indicates whether this provider can describe the given type.
     *
     * @param type the type to test
     * @return {@code true} if this provider supports the type
     */
    boolean supports(Class<?> type);

}