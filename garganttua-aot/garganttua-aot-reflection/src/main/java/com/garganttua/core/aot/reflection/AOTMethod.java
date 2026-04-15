package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;

/**
 * AOT implementation of {@link IMethod}.
 *
 * <p>Stores pre-computed method metadata. The actual {@link Method} is resolved
 * lazily for invocation.</p>
 */
public class AOTMethod implements IMethod {

    private final String name;
    private final String declaringClassName;
    private final String returnTypeName;
    private final String[] parameterTypeNames;
    private final String[] parameterNames;
    private final int modifiers;
    private final Annotation[] annotations;
    private final boolean bridge;
    private final boolean defaultMethod;
    private final boolean varArgs;
    private final String[] exceptionTypeNames;

    // Lazy resolution
    private volatile IClass<?> resolvedDeclaringClass;
    private volatile IClass<?> resolvedReturnType;
    private volatile IClass<?>[] resolvedParameterTypes;
    private volatile IClass<?>[] resolvedExceptionTypes;
    private volatile Method resolvedMethod;

    public AOTMethod(String name, String declaringClassName, String returnTypeName,
                     String[] parameterTypeNames, String[] parameterNames,
                     int modifiers, Annotation[] annotations,
                     boolean bridge, boolean defaultMethod, boolean varArgs,
                     String[] exceptionTypeNames) {
        this.name = name;
        this.declaringClassName = declaringClassName;
        this.returnTypeName = returnTypeName;
        this.parameterTypeNames = parameterTypeNames != null ? parameterTypeNames.clone() : new String[0];
        this.parameterNames = parameterNames != null ? parameterNames.clone() : new String[0];
        this.modifiers = modifiers;
        this.annotations = annotations != null ? annotations.clone() : new Annotation[0];
        this.bridge = bridge;
        this.defaultMethod = defaultMethod;
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
        return name;
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
        resolveMethod().setAccessible(flag);
    }

    @Override
    public boolean trySetAccessible() {
        return resolveMethod().trySetAccessible();
    }

    @Override
    public boolean canAccess(Object obj) {
        return resolveMethod().canAccess(obj);
    }

    // --- IGenericDeclaration ---

    @Override
    @SuppressWarnings("unchecked")
    public ITypeVariable<?>[] getTypeParameters() {
        return new ITypeVariable[0];
    }

    // --- Return type ---

    @Override
    public IClass<?> getReturnType() {
        IClass<?> cached = resolvedReturnType;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedReturnType == null) {
                try {
                    resolvedReturnType = IClass.forName(returnTypeName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot resolve return type: " + returnTypeName, e);
                }
            }
            return resolvedReturnType;
        }
    }

    @Override
    public Type getGenericReturnType() {
        return (Type) getReturnType().getType();
    }

    // --- Method-specific properties ---

    @Override
    public boolean isBridge() {
        return bridge;
    }

    @Override
    public boolean isDefault() {
        return defaultMethod;
    }

    @Override
    public Object getDefaultValue() {
        return resolveMethod().getDefaultValue();
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
        sb.append(returnTypeName).append(' ');
        sb.append(declaringClassName).append('.').append(name);
        sb.append('(');
        sb.append(Arrays.stream(parameterTypeNames).collect(Collectors.joining(",")));
        sb.append(')');
        return sb.toString();
    }

    // --- Invocation ---

    @Override
    public Object invoke(Object obj, Object... args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return resolveMethod().invoke(obj, args);
    }

    // --- Annotated types ---

    @Override
    public AnnotatedType getAnnotatedReturnType() {
        return resolveMethod().getAnnotatedReturnType();
    }

    @Override
    public AnnotatedType[] getAnnotatedParameterTypes() {
        return resolveMethod().getAnnotatedParameterTypes();
    }

    @Override
    public AnnotatedType[] getAnnotatedExceptionTypes() {
        return resolveMethod().getAnnotatedExceptionTypes();
    }

    @Override
    public AnnotatedType getAnnotatedReceiverType() {
        return resolveMethod().getAnnotatedReceiverType();
    }

    // --- AnnotatedElement ---

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
        for (Annotation a : annotations) {
            if (a.annotationType().getName().equals(annotationClass.getName())) {
                return (T) a;
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

    private Method resolveMethod() {
        Method cached = resolvedMethod;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedMethod == null) {
                try {
                    Class<?> clazz = Class.forName(declaringClassName);
                    Class<?>[] paramClasses = new Class<?>[parameterTypeNames.length];
                    for (int i = 0; i < parameterTypeNames.length; i++) {
                        paramClasses[i] = resolveRawClass(parameterTypeNames[i]);
                    }
                    resolvedMethod = clazz.getDeclaredMethod(name, paramClasses);
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    throw new IllegalStateException(
                            "Cannot resolve method: " + declaringClassName + "." + name, e);
                }
            }
            return resolvedMethod;
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

    static Class<?> resolveRawClass(String className) throws ClassNotFoundException {
        return switch (className) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> Class.forName(className);
        };
    }

    // --- Object overrides ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof AOTMethod other) {
            return declaringClassName.equals(other.declaringClassName)
                    && name.equals(other.name)
                    && Arrays.equals(parameterTypeNames, other.parameterTypeNames);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 31 * declaringClassName.hashCode() + name.hashCode();
        h = 31 * h + Arrays.hashCode(parameterTypeNames);
        return h;
    }

    @Override
    public String toString() {
        return declaringClassName + "." + name + "(" +
                Arrays.stream(parameterTypeNames).collect(Collectors.joining(", ")) + ")";
    }
}
