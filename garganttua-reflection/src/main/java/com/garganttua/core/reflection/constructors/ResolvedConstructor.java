package com.garganttua.core.reflection.constructors;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IParameter;

public record ResolvedConstructor<T>(IConstructor<T> constructor) implements IConstructor<T> {

    public IClass<T> constructedType() {
        return constructor.getDeclaringClass();
    }

    public boolean isVarArgs() {
        return constructor.isVarArgs();
    }

    public int parameterCount() {
        return constructor.getParameterCount();
    }

    public boolean matches(IClass<?>... parameterTypes) {
        IClass<?>[] actualParams = constructor.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length == 0) {
            return actualParams.length == 0;
        }
        if (actualParams.length != parameterTypes.length) {
            return false;
        }
        for (int i = 0; i < actualParams.length; i++) {
            if (!actualParams[i].isAssignableFrom(parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean matches(IConstructor<?> other) {
        return this.constructor.equals(other);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResolvedConstructor<?> other)) return false;
        return java.util.Objects.equals(constructor, other.constructor);
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(constructor);
    }

    // --- Member ---

    @Override
    public IClass<T> getDeclaringClass() {
        return constructor.getDeclaringClass();
    }

    @Override
    public String getName() {
        return constructor.getName();
    }

    @Override
    public int getModifiers() {
        return constructor.getModifiers();
    }

    @Override
    public boolean isSynthetic() {
        return constructor.isSynthetic();
    }

    // --- AccessibleObject ---

    @Override
    public void setAccessible(boolean flag) {
        constructor.setAccessible(flag);
    }

    @Override
    public boolean trySetAccessible() {
        return constructor.trySetAccessible();
    }

    @Override
    public boolean canAccess(Object obj) {
        return constructor.canAccess(obj);
    }

    // --- GenericDeclaration ---

    @Override
    public TypeVariable<?>[] getTypeParameters() {
        return constructor.getTypeParameters();
    }

    // --- Parameters ---

    @Override
    public IClass<?>[] getParameterTypes() {
        return constructor.getParameterTypes();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return constructor.getGenericParameterTypes();
    }

    @Override
    public int getParameterCount() {
        return constructor.getParameterCount();
    }

    @Override
    public IParameter[] getParameters() {
        return constructor.getParameters();
    }

    // --- Exceptions ---

    @Override
    public IClass<?>[] getExceptionTypes() {
        return constructor.getExceptionTypes();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return constructor.getGenericExceptionTypes();
    }

    // --- Constructor properties ---

    @Override
    public String toGenericString() {
        return constructor.toGenericString();
    }

    // --- Instantiation ---

    @Override
    public T newInstance(Object... initargs)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return constructor.newInstance(initargs);
    }

    // --- Annotated types ---

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return constructor.getAnnotatedReturnType();
    }

    @Override
    public AnnotatedType[] getAnnotatedParameterTypes() {
        return constructor.getAnnotatedParameterTypes();
    }

    @Override
    public AnnotatedType[] getAnnotatedExceptionTypes() {
        return constructor.getAnnotatedExceptionTypes();
    }

    @Override
    public AnnotatedType getAnnotatedReceiverType() {
        return constructor.getAnnotatedReceiverType();
    }

    // --- AnnotatedElement (IClass overloads) ---

    @Override
    public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
        return constructor.isAnnotationPresent(annotationClass);
    }

    @Override
    public <A extends Annotation> A getAnnotation(IClass<A> annotationClass) {
        return constructor.getAnnotation(annotationClass);
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(IClass<A> annotationClass) {
        return constructor.getAnnotationsByType(annotationClass);
    }

    @Override
    public <A extends Annotation> A getDeclaredAnnotation(IClass<A> annotationClass) {
        return constructor.getDeclaredAnnotation(annotationClass);
    }

    @Override
    public <A extends Annotation> A[] getDeclaredAnnotationsByType(IClass<A> annotationClass) {
        return constructor.getDeclaredAnnotationsByType(annotationClass);
    }

    // --- AnnotatedElement (Class overloads from java.lang.reflect.AnnotatedElement) ---

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return constructor.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return constructor.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return constructor.getDeclaredAnnotations();
    }

    @Override
    public String toString() {
        return "ResolvedConstructor[" + constructor.getDeclaringClass().getSimpleName() + "("
                + formatParameterTypes() + ")]";
    }

    private String formatParameterTypes() {
        IClass<?>[] paramTypes = constructor.getParameterTypes();
        if (paramTypes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            sb.append(paramTypes[i].getSimpleName());
            if (i < paramTypes.length - 1) sb.append(", ");
        }
        return sb.toString();
    }
}
