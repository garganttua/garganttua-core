package com.garganttua.core.reflection.methods;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;
import com.garganttua.core.reflection.ObjectAddress;

public record ResolvedMethod(ObjectAddress address, List<Object> methodPath) implements IMethod {

    private IMethod method() {
        return (IMethod) methodPath.getLast();
    }

    /**
     * Returns the owner type (declaring class) of the resolved method.
     */
    public IClass<?> ownerType() {
        return method().getDeclaringClass();
    }

    /**
     * Returns the return type of the resolved method.
     */
    public IClass<?> returnType() {
        return method().getReturnType();
    }

    /**
     * Returns whether the resolved method is static.
     */
    public boolean isStatic() {
        return Modifier.isStatic(method().getModifiers());
    }

    boolean matches(IMethod other) {
        return this.method().equals(other);
    }

    boolean matches(IClass<?> ownerType, IClass<?> returnType, IClass<?>[] parameterTypes) {
        if( !method().getDeclaringClass().isAssignableFrom(ownerType) )
            return false;

        if (returnType != null && !method().getReturnType().isAssignableFrom(returnType)) {
            return false;
        }

        if ((parameterTypes == null || parameterTypes.length == 0) && method().getParameterCount() != 0) {
            return false;
        }

        if (parameterTypes != null) {
            IClass<?>[] actualParams = method().getParameterTypes();
            if (actualParams.length != parameterTypes.length) {
                return false;
            }
            for (int i = 0; i < actualParams.length; i++) {
                if (!actualParams[i].isAssignableFrom(parameterTypes[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResolvedMethod(ObjectAddress otherAddress, List<Object> otherMethodPath))) return false;
        return java.util.Objects.equals(address, otherAddress)
                && java.util.Objects.equals(methodPath, otherMethodPath);
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(address, methodPath);
    }

    // --- Member ---

    @Override
    public IClass<?> getDeclaringClass() {
        return method().getDeclaringClass();
    }

    @Override
    public String getName() {
        return method().getName();
    }

    @Override
    public int getModifiers() {
        return method().getModifiers();
    }

    @Override
    public boolean isSynthetic() {
        return method().isSynthetic();
    }

    // --- AccessibleObject ---

    @Override
    public void setAccessible(boolean flag) {
        method().setAccessible(flag);
    }

    @Override
    public boolean trySetAccessible() {
        return method().trySetAccessible();
    }

    @Override
    public boolean canAccess(Object obj) {
        return method().canAccess(obj);
    }

    // --- GenericDeclaration ---

    @Override
    public ITypeVariable<?>[] getTypeParameters() {
        return method().getTypeParameters();
    }

    // --- Return type ---

    @Override
    public IClass<?> getReturnType() {
        return method().getReturnType();
    }

    @Override
    public Type getGenericReturnType() {
        return method().getGenericReturnType();
    }

    // --- Parameters ---

    @Override
    public IClass<?>[] getParameterTypes() {
        return method().getParameterTypes();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return method().getGenericParameterTypes();
    }

    @Override
    public int getParameterCount() {
        return method().getParameterCount();
    }

    @Override
    public IParameter[] getParameters() {
        return method().getParameters();
    }

    // --- Exceptions ---

    @Override
    public IClass<?>[] getExceptionTypes() {
        return method().getExceptionTypes();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return method().getGenericExceptionTypes();
    }

    // --- Method properties ---

    @Override
    public boolean isVarArgs() {
        return method().isVarArgs();
    }

    @Override
    public boolean isBridge() {
        return method().isBridge();
    }

    @Override
    public boolean isDefault() {
        return method().isDefault();
    }

    @Override
    public Object getDefaultValue() {
        return method().getDefaultValue();
    }

    @Override
    public String toGenericString() {
        return method().toGenericString();
    }

    // --- Invocation ---

    @Override
    public Object invoke(Object obj, Object... args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return method().invoke(obj, args);
    }

    // --- Annotated types ---

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return method().getAnnotatedReturnType();
    }

    @Override
    public AnnotatedType[] getAnnotatedParameterTypes() {
        return method().getAnnotatedParameterTypes();
    }

    @Override
    public AnnotatedType[] getAnnotatedExceptionTypes() {
        return method().getAnnotatedExceptionTypes();
    }

    @Override
    public AnnotatedType getAnnotatedReceiverType() {
        return method().getAnnotatedReceiverType();
    }

    // --- AnnotatedElement (IClass overloads) ---

    @Override
    public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
        return method().isAnnotationPresent(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
        return method().getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass) {
        return method().getAnnotationsByType(annotationClass);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass) {
        return method().getDeclaredAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass) {
        return method().getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return method().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return method().getDeclaredAnnotations();
    }

    // --- IAnnotatedElement ---

    @Override
    public IReflection reflection() {
        return method().reflection();
    }

    @Override
    public String toString() {
        return "ResolvedMethod[" + address + " -> " + method() + "]";
    }
}
