package com.garganttua.core.nativve;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;

/**
 * Builder interface for constructing reflection configuration entries for GraalVM native images.
 *
 * <p>
 * {@code IReflectionConfigurationEntryBuilder} provides a fluent API for building reflection
 * configuration entries. It supports configuring entire categories (all constructors, all methods,
 * all fields) as well as individual members. It also supports annotation-based filtering.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * IReflectionConfigurationEntry entry = builder
 *     .queryAllDeclaredConstructors(true)
 *     .queryAllDeclaredMethods(true)
 *     .field("serviceName")
 *     .field("serviceId")
 *     .method("initialize")
 *     .fieldsAnnotatedWith(Inject.class)
 *     .methodsAnnotatedWith(PostConstruct.class)
 *     .build();
 *
 * // Selective configuration
 * IReflectionConfigurationEntry specificEntry = builder
 *     .field("username")
 *     .method("getPassword")
 *     .constructor(UserService.class, String.class, String.class)
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IReflectionConfigurationEntry
 * @see IReflectionConfiguration
 */
public interface IReflectionConfigurationEntryBuilder extends IAutomaticBuilder<IReflectionConfigurationEntryBuilder, IReflectionConfigurationEntry> {

    /**
     * Enables or disables querying all declared constructors via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder queryAllDeclaredConstructors(boolean value);

    /**
     * Enables or disables querying all public constructors via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder queryAllPublicConstructors(boolean value);

    /**
     * Enables or disables querying all declared methods via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder queryAllDeclaredMethods(boolean value);

    /**
     * Enables or disables querying all public methods via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder queryAllPublicMethods(boolean value);

    /**
     * Enables or disables access to all declared inner classes via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder allDeclaredClasses(boolean value);

    /**
     * Enables or disables access to all public inner classes via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder allPublicClasses(boolean value);

    /**
     * Enables or disables access to all declared fields via reflection.
     *
     * @param value {@code true} to enable, {@code false} to disable
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder allDeclaredFields(boolean value);

    /**
     * Adds a field by name for reflection access.
     *
     * @param fieldName the name of the field
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder field(String fieldName);

    /**
     * Adds a field for reflection access.
     *
     * @param field the field to register (IField)
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder field(IField field);

    /**
     * Adds a method by name and parameter types for reflection access.
     *
     * @param methodName the name of the method
     * @param parameterType the parameter types of the method
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder method(String methodName, IClass<?> ...parameterType);

    /**
     * Adds a method for reflection access.
     *
     * @param method the method to register
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder method(IMethod method);

    /**
     * Adds a constructor by name and parameter types for reflection access.
     *
     * @param constructorName the name of the constructor (class name)
     * @param parameterType the parameter types of the constructor
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder constructor(String constructorName, IClass<?> ...parameterType);

    /**
     * Adds a constructor for reflection access.
     *
     * @param ctor the constructor to register
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder constructor(IConstructor<?> ctor);

    /**
     * Adds all fields annotated with the specified annotation for reflection access.
     *
     * @param annotation the annotation class to search for
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder fieldsAnnotatedWith(IClass<? extends Annotation> annotation);

    /**
     * Adds all methods annotated with the specified annotation for reflection access.
     *
     * @param annotation the annotation class to search for
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder methodsAnnotatedWith(IClass<? extends Annotation> annotation);

    /**
     * Removes a field by name from reflection access.
     *
     * @param fieldName the name of the field to remove
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeField(String fieldName);

    /**
     * Removes a field from reflection access.
     *
     * @param field the field to remove
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeField(IField field);

    /**
     * Removes a method by name and parameter types from reflection access.
     *
     * @param methodName the name of the method to remove
     * @param parameterType the parameter types of the method
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeMethod(String methodName, IClass<?> ...parameterType);

    /**
     * Removes a method from reflection access.
     *
     * @param method the method to remove
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeMethod(IMethod method);

    /**
     * Removes a constructor by name and parameter types from reflection access.
     *
     * @param constructorName the name of the constructor to remove (class name)
     * @param parameterType the parameter types of the constructor
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeConstructor(String constructorName, IClass<?> ...parameterType);

    /**
     * Removes a constructor from reflection access.
     *
     * @param ctor the constructor to remove
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeConstructor(IConstructor<?> ctor);

    /**
     * Removes all fields annotated with the specified annotation from reflection access.
     *
     * @param annotation the annotation class to search for
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeFieldsAnnotatedWith(IClass<? extends Annotation> annotation);

    /**
     * Removes all methods annotated with the specified annotation from reflection access.
     *
     * @param annotation the annotation class to search for
     * @return this builder for method chaining
     */
    IReflectionConfigurationEntryBuilder removeMethodAnnotatedWith(IClass<? extends Annotation> annotation);

}
