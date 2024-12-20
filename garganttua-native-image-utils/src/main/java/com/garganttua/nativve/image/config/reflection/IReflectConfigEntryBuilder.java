package com.garganttua.nativve.image.config.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface IReflectConfigEntryBuilder {

    IReflectConfigEntryBuilder queryAllDeclaredConstructors(boolean value);

    IReflectConfigEntryBuilder queryAllPublicConstructors(boolean value);

    IReflectConfigEntryBuilder queryAllDeclaredMethods(boolean value);

    IReflectConfigEntryBuilder queryAllPublicMethods(boolean value);

    IReflectConfigEntryBuilder allDeclaredClasses(boolean value);

    IReflectConfigEntryBuilder allPublicClasses(boolean value);

    IReflectConfigEntryBuilder allDeclaredFields(boolean value);
    
    IReflectConfigEntryBuilder field(String fieldName);

    IReflectConfigEntryBuilder field(Field field);

    IReflectConfigEntryBuilder method(String methodName, Class<?> ...parameterType);

    IReflectConfigEntryBuilder method(Method method);

    IReflectConfigEntryBuilder constructor(String constructorName, Class<?> ...parameterType);

    IReflectConfigEntryBuilder constructor(Constructor<?> ctor);

    IReflectConfigEntryBuilder fieldsAnnotatedWith(Class<? extends Annotation> annotation);

    IReflectConfigEntryBuilder methodsAnnotatedWith(Class<? extends Annotation> annotation);

    IReflectConfigEntryBuilder removeField(String fieldName);

    IReflectConfigEntryBuilder removeField(Field field);

    IReflectConfigEntryBuilder removeMethod(String methodName, Class<?> ...parameterType);

    IReflectConfigEntryBuilder removeMethod(Method method);

    IReflectConfigEntryBuilder removeConstructor(String constructorName, Class<?> ...parameterType);

    IReflectConfigEntryBuilder removeConstructor(Constructor<?> ctor);

    IReflectConfigEntryBuilder removeFieldsAnnotatedWith(Class<? extends Annotation> annotation);

    IReflectConfigEntryBuilder removeMethodAnnotatedWith(Class<? extends Annotation> annotation);

    ReflectConfigEntry build();

	
}
