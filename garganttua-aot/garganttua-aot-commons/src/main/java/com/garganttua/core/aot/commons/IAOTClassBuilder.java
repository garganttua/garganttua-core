package com.garganttua.core.aot.commons;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;

/**
 * Fluent builder for customizing AOT-generated {@link IClass} descriptors.
 *
 * <p>The {@code DirectBinderGenerator} annotation processor creates an
 * {@code IAOTClassBuilder} per {@code @Reflected} class and pre-configures
 * it from the annotation attributes. Users can also create builders
 * programmatically for dynamic cases (plugins, extensions).</p>
 *
 * <p>This follows the same pattern as
 * {@code IReflectionConfigurationEntryBuilder} in the native module.</p>
 *
 * @param <T> the type being described
 */
public interface IAOTClassBuilder<T> extends IAutomaticBuilder<IAOTClassBuilder<T>, IClass<T>> {

    // --- Field addition / removal ---

    /**
     * Includes the field with the given name in the descriptor.
     *
     * @param fieldName the simple field name
     * @return this builder
     */
    IAOTClassBuilder<T> field(String fieldName);

    /**
     * Includes the given field in the descriptor.
     *
     * @param field the field to include
     * @return this builder
     */
    IAOTClassBuilder<T> field(IField field);

    /**
     * Includes every field carrying the given annotation.
     *
     * @param annotation the annotation type to match
     * @return this builder
     */
    IAOTClassBuilder<T> fieldsAnnotatedWith(IClass<? extends Annotation> annotation);

    /**
     * Excludes the field with the given name from the descriptor.
     *
     * @param fieldName the simple field name
     * @return this builder
     */
    IAOTClassBuilder<T> removeField(String fieldName);

    // --- Method addition / removal ---

    /**
     * Includes the method matching the given name and parameter types.
     *
     * @param methodName the method name
     * @param parameterTypes the parameter types, in declaration order
     * @return this builder
     */
    IAOTClassBuilder<T> method(String methodName, IClass<?>... parameterTypes);

    /**
     * Includes the given method in the descriptor.
     *
     * @param method the method to include
     * @return this builder
     */
    IAOTClassBuilder<T> method(IMethod method);

    /**
     * Includes every method carrying the given annotation.
     *
     * @param annotation the annotation type to match
     * @return this builder
     */
    IAOTClassBuilder<T> methodsAnnotatedWith(IClass<? extends Annotation> annotation);

    /**
     * Excludes the method matching the given name and parameter types.
     *
     * @param methodName the method name
     * @param parameterTypes the parameter types, in declaration order
     * @return this builder
     */
    IAOTClassBuilder<T> removeMethod(String methodName, IClass<?>... parameterTypes);

    // --- Constructor addition / removal ---

    /**
     * Includes the constructor matching the given parameter types.
     *
     * @param parameterTypes the parameter types, in declaration order
     * @return this builder
     */
    IAOTClassBuilder<T> constructor(IClass<?>... parameterTypes);

    /**
     * Includes the given constructor in the descriptor.
     *
     * @param constructor the constructor to include
     * @return this builder
     */
    IAOTClassBuilder<T> constructor(IConstructor<?> constructor);

    /**
     * Excludes the constructor matching the given parameter types.
     *
     * @param parameterTypes the parameter types, in declaration order
     * @return this builder
     */
    IAOTClassBuilder<T> removeConstructor(IClass<?>... parameterTypes);

    // --- Global flags ---

    /**
     * Registers all declared constructors for reflection.
     *
     * @param value {@code true} to include every declared constructor
     * @return this builder
     */
    IAOTClassBuilder<T> queryAllDeclaredConstructors(boolean value);

    /**
     * Registers all public constructors for reflection.
     *
     * @param value {@code true} to include every public constructor
     * @return this builder
     */
    IAOTClassBuilder<T> queryAllPublicConstructors(boolean value);

    /**
     * Registers all declared methods for reflection.
     *
     * @param value {@code true} to include every declared method
     * @return this builder
     */
    IAOTClassBuilder<T> queryAllDeclaredMethods(boolean value);

    /**
     * Registers all public methods for reflection.
     *
     * @param value {@code true} to include every public method
     * @return this builder
     */
    IAOTClassBuilder<T> queryAllPublicMethods(boolean value);

    /**
     * Registers all declared fields for reflection.
     *
     * @param value {@code true} to include every declared field
     * @return this builder
     */
    IAOTClassBuilder<T> allDeclaredFields(boolean value);

    /**
     * Registers all public fields for reflection.
     *
     * @param value {@code true} to include every public field
     * @return this builder
     */
    IAOTClassBuilder<T> allPublicFields(boolean value);

    /**
     * Registers all declared member classes for reflection.
     *
     * @param value {@code true} to include every declared nested class
     * @return this builder
     */
    IAOTClassBuilder<T> allDeclaredClasses(boolean value);

}
