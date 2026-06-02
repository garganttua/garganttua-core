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

    /**
     * Creates an AOT method descriptor from pre-computed metadata.
     *
     * <p>All arrays are defensively cloned; {@code null} arrays are normalised to
     * empty ones. The live {@link Method} is resolved lazily on first invocation.</p>
     *
     * @param name the method name
     * @param declaringClassName binary name of the declaring class
     * @param returnTypeName binary name of the return type
     * @param parameterTypeNames binary names of the parameter types, in order
     * @param parameterNames the parameter names, in order (may be shorter than the types)
     * @param annotations the declared annotations present on the method
     * @param bridge whether the method is a bridge method
     * @param defaultMethod whether the method is a {@code default} interface method
     * @param exceptionTypeNames binary names of the declared checked exception types
     */
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

    /**
     * Synthesizes an {@code AOTMethod} descriptor from a runtime
     * {@link java.lang.reflect.Method}. Used by the lazy-fallback path in
     * {@link AOTClass#getMethod} and friends when the precomputed descriptor
     * misses but the actual class is reachable via {@code Class.forName(...)}.
     *
     * <p>The synthesised method carries the same metadata as a
     * processor-generated one, so downstream code that introspects name,
     * modifiers, parameter types, annotations, etc. sees no difference.</p>
     */
    public static AOTMethod synthesizeFrom(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        String[] paramTypeNames = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypeNames[i] = paramTypes[i].getName();
        }
        java.lang.reflect.Parameter[] params = method.getParameters();
        String[] paramNames = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            paramNames[i] = params[i].getName();
        }
        Class<?>[] exTypes = method.getExceptionTypes();
        String[] exTypeNames = new String[exTypes.length];
        for (int i = 0; i < exTypes.length; i++) {
            exTypeNames[i] = exTypes[i].getName();
        }
        return new AOTMethod(
                method.getName(),
                method.getDeclaringClass().getName(),
                method.getReturnType().getName(),
                paramTypeNames,
                paramNames,
                method.getModifiers(),
                method.getAnnotations(),
                method.isBridge(),
                method.isDefault(),
                method.isVarArgs(),
                exTypeNames);
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
        Annotation[][] paramAnnotations = resolveParameterAnnotationsOrNull();
        IParameter[] result = new IParameter[parameterTypeNames.length];
        for (int i = 0; i < parameterTypeNames.length; i++) {
            String pName = (parameterNames.length > i) ? parameterNames[i] : ("arg" + i);
            Annotation[] annos = (paramAnnotations != null
                    && paramAnnotations.length > i && paramAnnotations[i] != null)
                    ? paramAnnotations[i] : new Annotation[0];
            result[i] = new AOTParameter(pName, parameterTypeNames[i], 0,
                    parameterNames.length > i, false, false, (varArgs && i == parameterTypeNames.length - 1),
                    annos);
        }
        return result;
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        Annotation[][] resolved = resolveParameterAnnotationsOrNull();
        if (resolved != null) {
            return resolved;
        }
        Annotation[][] result = new Annotation[parameterTypeNames.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Annotation[0];
        }
        return result;
    }

    /**
     * Parameter-level annotations are not baked into the generated descriptor
     * (the source generator emits parameter types/names only), so recover them
     * from the live {@link Method} — exactly as {@code AOTClass} sources its
     * class annotations from {@code MyClass.class.getAnnotations()}. Without
     * this, RUNTIME-retained marker annotations on parameters are invisible
     * under AOT: e.g. {@code @Nullable} on {@code observe(…, Integer code)}
     * would be lost, the expression layer would treat the argument as
     * non-nullable, and a null value would abort the whole call instead of
     * passing through. Degrades to {@code null} (→ empty arrays) when the live
     * method cannot be resolved, so AOT-only setups are never worse off than
     * the previous always-empty behaviour.
     */
    private Annotation[][] resolveParameterAnnotationsOrNull() {
        try {
            return resolveMethod().getParameterAnnotations();
        } catch (RuntimeException e) {
            return null;
        }
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
        // Strip nested "[]" suffixes so we resolve "byte[][]" as byte.class
        // then wrap with arrayType() twice. AOT processor emits Java-style
        // names ("byte[]", "java.lang.String[]") rather than JVM internals
        // ("[B", "[Ljava.lang.String;") which Class.forName can't parse for
        // primitive arrays.
        int arrayDepth = 0;
        String elementName = className;
        while (elementName.endsWith("[]")) {
            arrayDepth++;
            elementName = elementName.substring(0, elementName.length() - 2);
        }
        Class<?> element = switch (elementName) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> Class.forName(elementName);
        };
        for (int i = 0; i < arrayDepth; i++) {
            element = element.arrayType();
        }
        return element;
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
