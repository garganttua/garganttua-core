package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;

/**
 * AOT implementation of {@link IConstructor}.
 *
 * <p>Stores pre-computed constructor metadata. The actual {@link Constructor} is resolved
 * lazily for instantiation.</p>
 *
 * @param <T> the class in which the constructor is declared
 */
public class AOTConstructor<T> implements IConstructor<T> {

    private final String declaringClassName;
    private final String[] parameterTypeNames;
    private final String[] parameterNames;
    private final int modifiers;
    private final Annotation[] annotations;
    private final boolean varArgs;
    private final String[] exceptionTypeNames;

    // Lazy resolution
    private volatile IClass<?> resolvedDeclaringClass;
    private volatile IClass<?>[] resolvedParameterTypes;
    private volatile IClass<?>[] resolvedExceptionTypes;
    private volatile Constructor<?> resolvedConstructor;

    public AOTConstructor(String declaringClassName, String[] parameterTypeNames,
                          String[] parameterNames, int modifiers,
                          Annotation[] annotations, boolean varArgs,
                          String[] exceptionTypeNames) {
        this.declaringClassName = declaringClassName;
        this.parameterTypeNames = parameterTypeNames != null ? parameterTypeNames.clone() : new String[0];
        this.parameterNames = parameterNames != null ? parameterNames.clone() : new String[0];
        this.modifiers = modifiers;
        this.annotations = annotations != null ? annotations.clone() : new Annotation[0];
        this.varArgs = varArgs;
        this.exceptionTypeNames = exceptionTypeNames != null ? exceptionTypeNames.clone() : new String[0];
    }

    // --- IMember ---

    @Override
    public IClass<?> getDeclaringClass() {
        IClass<?> cached = resolvedDeclaringClass;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedDeclaringClass == null) {
                try {
                    resolvedDeclaringClass = IClass.forName(declaringClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot resolve declaring class: " + declaringClassName, e);
                }
            }
            return resolvedDeclaringClass;
        }
    }

    @Override
    public String getName() {
        return declaringClassName;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public boolean isSynthetic() {
        return (modifiers & 0x00001000) != 0; // ACC_SYNTHETIC
    }

    // --- AccessibleObject ---

    @Override
    public void setAccessible(boolean flag) {
        resolveConstructor().setAccessible(flag);
    }

    @Override
    public boolean trySetAccessible() {
        return resolveConstructor().trySetAccessible();
    }

    @Override
    public boolean canAccess(Object obj) {
        return resolveConstructor().canAccess(obj);
    }

    // --- IGenericDeclaration ---

    @Override
    @SuppressWarnings("unchecked")
    public ITypeVariable<?>[] getTypeParameters() {
        return new ITypeVariable[0];
    }

    // --- Parameters ---

    @Override
    public IClass<?>[] getParameterTypes() {
        IClass<?>[] cached = resolvedParameterTypes;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedParameterTypes == null) {
                resolvedParameterTypes = resolveClassArray(parameterTypeNames);
            }
            return resolvedParameterTypes;
        }
    }

    @Override
    public Type[] getGenericParameterTypes() {
        IClass<?>[] paramTypes = getParameterTypes();
        Type[] result = new Type[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            result[i] = (Type) paramTypes[i].getType();
        }
        return result;
    }

    @Override
    public int getParameterCount() {
        return parameterTypeNames.length;
    }

    @Override
    public IParameter[] getParameters() {
        IParameter[] result = new IParameter[parameterTypeNames.length];
        for (int i = 0; i < parameterTypeNames.length; i++) {
            String pName = (parameterNames.length > i) ? parameterNames[i] : ("arg" + i);
            result[i] = new AOTParameter(pName, parameterTypeNames[i], 0,
                    parameterNames.length > i, false, false, (varArgs && i == parameterTypeNames.length - 1),
                    new Annotation[0]);
        }
        return result;
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        Annotation[][] result = new Annotation[parameterTypeNames.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Annotation[0];
        }
        return result;
    }

    // --- Exceptions ---

    @Override
    public IClass<?>[] getExceptionTypes() {
        IClass<?>[] cached = resolvedExceptionTypes;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedExceptionTypes == null) {
                resolvedExceptionTypes = resolveClassArray(exceptionTypeNames);
            }
            return resolvedExceptionTypes;
        }
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        IClass<?>[] exTypes = getExceptionTypes();
        Type[] result = new Type[exTypes.length];
        for (int i = 0; i < exTypes.length; i++) {
            result[i] = (Type) exTypes[i].getType();
        }
        return result;
    }

    // --- Executable properties ---

    @Override
    public boolean isVarArgs() {
        return varArgs;
    }

    @Override
    public String toGenericString() {
        StringBuilder sb = new StringBuilder();
        String mod = Modifier.toString(modifiers);
        if (!mod.isEmpty()) sb.append(mod).append(' ');
        sb.append(declaringClassName);
        sb.append('(');
        sb.append(Arrays.stream(parameterTypeNames).collect(Collectors.joining(",")));
        sb.append(')');
        return sb.toString();
    }

    // --- Instantiation ---

    @Override
    @SuppressWarnings("unchecked")
    public T newInstance(Object... initargs)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return (T) resolveConstructor().newInstance(initargs);
    }

    // --- Annotated types ---

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return resolveConstructor().getAnnotatedReturnType();
    }

    @Override
    public AnnotatedType[] getAnnotatedParameterTypes() {
        return resolveConstructor().getAnnotatedParameterTypes();
    }

    @Override
    public AnnotatedType[] getAnnotatedExceptionTypes() {
        return resolveConstructor().getAnnotatedExceptionTypes();
    }

    @Override
    public AnnotatedType getAnnotatedReceiverType() {
        return resolveConstructor().getAnnotatedReceiverType();
    }

    // --- AnnotatedElement ---

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(IClass<A> annotationClass) {
        for (Annotation a : annotations) {
            if (a.annotationType().getName().equals(annotationClass.getName())) {
                return (A) a;
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations.clone();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotations.clone();
    }

    @Override
    public IReflection reflection() {
        return IClass.getReflection();
    }

    // --- Lazy resolution ---

    private Constructor<?> resolveConstructor() {
        Constructor<?> cached = resolvedConstructor;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedConstructor == null) {
                try {
                    Class<?> clazz = Class.forName(declaringClassName);
                    Class<?>[] paramClasses = new Class<?>[parameterTypeNames.length];
                    for (int i = 0; i < parameterTypeNames.length; i++) {
                        paramClasses[i] = AOTMethod.resolveRawClass(parameterTypeNames[i]);
                    }
                    resolvedConstructor = clazz.getDeclaredConstructor(paramClasses);
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    throw new IllegalStateException(
                            "Cannot resolve constructor: " + declaringClassName, e);
                }
            }
            return resolvedConstructor;
        }
    }

    private static IClass<?>[] resolveClassArray(String[] classNames) {
        IClass<?>[] result = new IClass<?>[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            try {
                result[i] = IClass.forName(classNames[i]);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot resolve class: " + classNames[i], e);
            }
        }
        return result;
    }

    // --- Object overrides ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof AOTConstructor<?> other) {
            return declaringClassName.equals(other.declaringClassName)
                    && Arrays.equals(parameterTypeNames, other.parameterTypeNames);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * declaringClassName.hashCode() + Arrays.hashCode(parameterTypeNames);
    }

    @Override
    public String toString() {
        return declaringClassName + "(" +
                Arrays.stream(parameterTypeNames).collect(Collectors.joining(", ")) + ")";
    }
}
