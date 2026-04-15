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

    IAOTClassBuilder<T> field(String fieldName);

    IAOTClassBuilder<T> field(IField field);

    IAOTClassBuilder<T> fieldsAnnotatedWith(IClass<? extends Annotation> annotation);

    IAOTClassBuilder<T> removeField(String fieldName);

    // --- Method addition / removal ---

    IAOTClassBuilder<T> method(String methodName, IClass<?>... parameterTypes);

    IAOTClassBuilder<T> method(IMethod method);

    IAOTClassBuilder<T> methodsAnnotatedWith(IClass<? extends Annotation> annotation);

    IAOTClassBuilder<T> removeMethod(String methodName, IClass<?>... parameterTypes);

    // --- Constructor addition / removal ---

    IAOTClassBuilder<T> constructor(IClass<?>... parameterTypes);

    IAOTClassBuilder<T> constructor(IConstructor<?> constructor);

    IAOTClassBuilder<T> removeConstructor(IClass<?>... parameterTypes);

    // --- Global flags ---

    IAOTClassBuilder<T> queryAllDeclaredConstructors(boolean value);

    IAOTClassBuilder<T> queryAllPublicConstructors(boolean value);

    IAOTClassBuilder<T> queryAllDeclaredMethods(boolean value);

    IAOTClassBuilder<T> queryAllPublicMethods(boolean value);

    IAOTClassBuilder<T> allDeclaredFields(boolean value);

    IAOTClassBuilder<T> allPublicFields(boolean value);

    IAOTClassBuilder<T> allDeclaredClasses(boolean value);

}
