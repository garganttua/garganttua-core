package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.aot.commons.IAOTClassDescriptor;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IRecordComponent;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;

/**
 * AOT implementation of {@link IClass} via {@link IAOTClassDescriptor}.
 *
 * <p>All structural metadata (fields, methods, constructors, annotations) is stored
 * as final fields populated at construction time by generated code. The underlying
 * {@link Class} object is resolved lazily via {@code Class.forName()} only when
 * operations require it (cast, isInstance, etc.).</p>
 *
 * @param <T> the type represented by this class descriptor
 */
public class AOTClass<T> implements IAOTClassDescriptor<T> {

    // --- Pre-computed metadata ---
    private final String name;
    private final String simpleName;
    private final String canonicalName;
    private final String packageName;
    private final int modifiers;
    private final String superclassName;
    private final String[] interfaceNames;
    private final AOTField[] fields;
    private final AOTMethod[] methods;
    private final AOTConstructor<?>[] constructors;
    private final Annotation[] annotations;
    private final boolean isInterface;
    private final boolean isArray;
    private final boolean isPrimitive;
    private final boolean isAnnotation;
    private final boolean isEnum;
    private final boolean isRecord;
    private final boolean isSealed;
    private final boolean isHidden;
    private final boolean isMemberClass;
    private final boolean isLocalClass;
    private final boolean isAnonymousClass;
    private final boolean isSynthetic;

    // --- Lazy resolution ---
    private volatile Class<T> resolvedClass;
    private volatile IClass<? super T> resolvedSuperclass;
    private volatile IClass<?>[] resolvedInterfaces;

    // --- On-demand live-class fallback (shallow descriptors only) ---
    // Synthesises AOT* members from the live Class<?> when this descriptor's
    // own member arrays are empty (see AOTLiveClassFallback). One per AOTClass.
    private final AOTLiveClassFallback liveFallback;

    /**
     * Creates a fully-populated AOT class descriptor from pre-computed metadata.
     *
     * <p>All arrays are defensively cloned; {@code null} arrays are normalised to
     * empty ones. The underlying {@link Class} is not touched here — it is resolved
     * lazily only when an operation needs the live type.</p>
     *
     * @param name fully-qualified binary name (or Java-source array name such as {@code "int[]"})
     * @param superclassName binary name of the superclass, or {@code null} for none
     * @param interfaceNames binary names of the directly-implemented interfaces
     * @param fields the declared field descriptors
     * @param methods the declared method descriptors
     * @param constructors the declared constructor descriptors
     * @param annotations the declared annotations present on the type
     */
    @SuppressWarnings("java:S107") // Constructor with many parameters is intentional for AOT
    public AOTClass(String name, String simpleName, String canonicalName, String packageName,
                    int modifiers, String superclassName, String[] interfaceNames,
                    AOTField[] fields, AOTMethod[] methods, AOTConstructor<?>[] constructors,
                    Annotation[] annotations,
                    boolean isInterface, boolean isArray, boolean isPrimitive,
                    boolean isAnnotation, boolean isEnum, boolean isRecord,
                    boolean isSealed, boolean isHidden, boolean isMemberClass,
                    boolean isLocalClass, boolean isAnonymousClass, boolean isSynthetic) {
        this.name = name;
        this.liveFallback = new AOTLiveClassFallback(name);
        this.simpleName = simpleName;
        this.canonicalName = canonicalName;
        this.packageName = packageName;
        this.modifiers = modifiers;
        this.superclassName = superclassName;
        this.interfaceNames = interfaceNames != null ? interfaceNames.clone() : new String[0];
        this.fields = fields != null ? fields.clone() : new AOTField[0];
        this.methods = methods != null ? methods.clone() : new AOTMethod[0];
        this.constructors = constructors != null ? constructors.clone() : new AOTConstructor[0];
        this.annotations = annotations != null ? annotations.clone() : new Annotation[0];
        this.isInterface = isInterface;
        this.isArray = isArray;
        this.isPrimitive = isPrimitive;
        this.isAnnotation = isAnnotation;
        this.isEnum = isEnum;
        this.isRecord = isRecord;
        this.isSealed = isSealed;
        this.isHidden = isHidden;
        this.isMemberClass = isMemberClass;
        this.isLocalClass = isLocalClass;
        this.isAnonymousClass = isAnonymousClass;
        this.isSynthetic = isSynthetic;
    }

    // --- Naming ---

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public String getTypeName() {
        return name;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toGenericString() {
        StringBuilder sb = new StringBuilder();
        String mod = Modifier.toString(modifiers);
        if (!mod.isEmpty()) sb.append(mod).append(' ');
        if (isInterface) {
            sb.append("interface ");
        } else if (isEnum) {
            sb.append("enum ");
        } else if (isRecord) {
            sb.append("record ");
        } else {
            sb.append("class ");
        }
        sb.append(name);
        return sb.toString();
    }

    @Override
    public String descriptorString() {
        if (isPrimitive) {
            return switch (name) {
                case "boolean" -> "Z";
                case "byte" -> "B";
                case "char" -> "C";
                case "short" -> "S";
                case "int" -> "I";
                case "long" -> "J";
                case "float" -> "F";
                case "double" -> "D";
                case "void" -> "V";
                default -> "L" + name.replace('.', '/') + ";";
            };
        }
        if (isArray) {
            return name.replace('.', '/');
        }
        return "L" + name.replace('.', '/') + ";";
    }

    // --- Modifiers & properties ---

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public boolean isInterface() {
        return isInterface;
    }

    @Override
    public boolean isArray() {
        return isArray;
    }

    @Override
    public boolean isPrimitive() {
        return isPrimitive;
    }

    @Override
    public boolean isAnnotation() {
        return isAnnotation;
    }

    @Override
    public boolean isSynthetic() {
        return isSynthetic;
    }

    @Override
    public boolean isEnum() {
        return isEnum;
    }

    @Override
    public boolean isRecord() {
        return isRecord;
    }

    @Override
    public boolean isSealed() {
        return isSealed;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    @Override
    public boolean isMemberClass() {
        return isMemberClass;
    }

    @Override
    public boolean isLocalClass() {
        return isLocalClass;
    }

    @Override
    public boolean isAnonymousClass() {
        return isAnonymousClass;
    }

    // --- Type hierarchy ---

    @Override
    @SuppressWarnings("unchecked")
    public IClass<? super T> getSuperclass() {
        if (superclassName == null) return null;
        IClass<? super T> cached = resolvedSuperclass;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedSuperclass == null) {
                try {
                    resolvedSuperclass = (IClass<? super T>) IClass.forName(superclassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot resolve superclass: " + superclassName, e);
                }
            }
            return resolvedSuperclass;
        }
    }

    @Override
    public IClass<?>[] getInterfaces() {
        IClass<?>[] cached = resolvedInterfaces;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedInterfaces == null) {
                resolvedInterfaces = new IClass<?>[interfaceNames.length];
                for (int i = 0; i < interfaceNames.length; i++) {
                    try {
                        resolvedInterfaces[i] = IClass.forName(interfaceNames[i]);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Cannot resolve interface: " + interfaceNames[i], e);
                    }
                }
            }
            return resolvedInterfaces;
        }
    }

    @Override
    public Type getGenericSuperclass() {
        return getTypeResolved().getGenericSuperclass();
    }

    @Override
    public Type[] getGenericInterfaces() {
        return getTypeResolved().getGenericInterfaces();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ITypeVariable<IClass<T>>[] getTypeParameters() {
        return new ITypeVariable[0];
    }

    // --- Type checks ---

    @Override
    public boolean isAssignableFrom(IClass<?> cls) {
        return getTypeResolved().isAssignableFrom((Class<?>) cls.getType());
    }

    @Override
    public boolean isAssignableFrom(Class<?> cls) {
        return getTypeResolved().isAssignableFrom(cls);
    }

    @Override
    public boolean isInstance(Object obj) {
        return getTypeResolved().isInstance(obj);
    }

    // --- Array ---

    @Override
    public IClass<?> getComponentType() {
        if (!isArray) return null;
        Class<?> ct = getTypeResolved().getComponentType();
        return ct == null ? null : IClass.getClass(ct);
    }

    @Override
    public IClass<?> componentType() {
        return getComponentType();
    }

    @Override
    public IClass<?> arrayType() {
        return IClass.getClass(getTypeResolved().arrayType());
    }

    @Override
    public Optional<ClassDesc> describeConstable() {
        return Optional.empty();
    }

    // --- Declared members (own declarations only) ---

    @Override
    public IField[] getDeclaredFields() {
        if (fields.length > 0) return fields.clone();
        IField[] fallback = liveFallback.declaredFields();
        return fallback != null ? fallback : fields.clone();
    }

    @Override
    public IMethod[] getDeclaredMethods() {
        if (methods.length > 0) return methods.clone();
        IMethod[] fallback = liveFallback.declaredMethods();
        return fallback != null ? fallback : methods.clone();
    }

    @Override
    public IConstructor<?>[] getDeclaredConstructors() {
        // Shallow descriptors always have at least the synthesised no-arg
        // ctor (when one exists on the live class). If we have more than one
        // ctor here, trust it. Otherwise enrich from the live class.
        if (constructors.length > 1) return constructors.clone();
        IConstructor<?>[] fallback = liveFallback.declaredConstructors();
        return fallback != null ? fallback : constructors.clone();
    }

    /**
     * The classes of every generator-emitted descriptor this AOTClass holds —
     * itself plus its raw {@code AOTField_*}/{@code AOTMethod_*}/
     * {@code AOTConstructor_*} INSTANCEs — for the native-image Feature to
     * declare initialize-at-build-time.
     *
     * <p>Deliberately reads the raw backing arrays, NOT
     * {@link #getDeclaredConstructors()} et al.: those fall back to live-
     * reflection synthesis when a descriptor is shallow (and notably for any
     * class with a single constructor, where {@code length <= 1}), returning
     * synthesized instances whose class is the base {@code AOTConstructor}
     * rather than the generated {@code AOTConstructor_X_0} that actually sits
     * in the image heap. Marking the wrong class left those heap objects
     * unreachable for build-time init and failed the native build.</p>
     */
    public java.util.List<Class<?>> descriptorClassesForBuildTimeInit() {
        java.util.List<Class<?>> out = new java.util.ArrayList<>();
        out.add(this.getClass());
        for (AOTField f : this.fields) {
            out.add(f.getClass());
        }
        for (AOTMethod m : this.methods) {
            out.add(m.getClass());
        }
        for (AOTConstructor<?> c : this.constructors) {
            out.add(c.getClass());
        }
        return out;
    }

    @Override
    public IField getDeclaredField(String fieldName) throws NoSuchFieldException, SecurityException {
        for (AOTField f : fields) {
            if (f.getName().equals(fieldName)) return f;
        }
        AOTField fallback = liveFallback.declaredField(fieldName);
        if (fallback != null) return fallback;
        throw new NoSuchFieldException(fieldName);
    }

    @Override
    public IMethod getDeclaredMethod(String methodName, IClass<?>... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        for (AOTMethod m : methods) {
            if (m.getName().equals(methodName) && parameterTypesMatch(m, parameterTypes)) {
                return m;
            }
        }
        // Fallback: synthesise from the live Class<?> on first miss. Covers
        // shallow descriptors (CoreInfrastructureSeed.synthesize) whose
        // methods array is empty. Pure-AOT mode on JVM works because
        // Class.forName + getDeclaredMethod are runtime-available. Native-
        // image consumers need the method in reflect-config (handled by the
        // GarganttuaAotFeature for seeded types).
        AOTMethod fallback = liveFallback.declaredMethod(methodName, parameterTypes);
        if (fallback != null) return fallback;
        throw new NoSuchMethodException(methodName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IConstructor<T> getDeclaredConstructor(IClass<?>... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        for (AOTConstructor<?> c : constructors) {
            if (constructorParameterTypesMatch(c, parameterTypes)) {
                return (IConstructor<T>) c;
            }
        }
        AOTConstructor<?> fallback = liveFallback.declaredConstructor(parameterTypes);
        if (fallback != null) return (IConstructor<T>) fallback;
        throw new NoSuchMethodException(name + ".<init>");
    }

    // --- Public members (including inherited) ---

    @Override
    public IField[] getFields() {
        // Return only public fields from this class and inherited
        List<IField> publicFields = new ArrayList<>();
        for (AOTField f : fields) {
            if (Modifier.isPublic(f.getModifiers())) {
                publicFields.add(f);
            }
        }
        // Add inherited public fields from superclass
        IClass<? super T> superclass = getSuperclass();
        if (superclass != null) {
            publicFields.addAll(Arrays.asList(superclass.getFields()));
        }
        return publicFields.toArray(new IField[0]);
    }

    @Override
    public IMethod[] getMethods() {
        List<IMethod> publicMethods = new ArrayList<>();
        for (AOTMethod m : methods) {
            if (Modifier.isPublic(m.getModifiers())) {
                publicMethods.add(m);
            }
        }
        IClass<? super T> superclass = getSuperclass();
        if (superclass != null) {
            publicMethods.addAll(Arrays.asList(superclass.getMethods()));
        }
        return publicMethods.toArray(new IMethod[0]);
    }

    @Override
    public IConstructor<?>[] getConstructors() {
        List<IConstructor<?>> publicCtors = new ArrayList<>();
        for (AOTConstructor<?> c : constructors) {
            if (Modifier.isPublic(c.getModifiers())) {
                publicCtors.add(c);
            }
        }
        return publicCtors.toArray(new IConstructor<?>[0]);
    }

    @Override
    public IField getField(String fieldName) throws NoSuchFieldException, SecurityException {
        // Search declared public fields first
        for (AOTField f : fields) {
            if (f.getName().equals(fieldName) && Modifier.isPublic(f.getModifiers())) {
                return f;
            }
        }
        // Live-class fallback before superclass walk — see getMethod for the
        // rationale (avoids the IReflection dependency for shallow descriptors).
        AOTField fallback = liveFallback.publicField(fieldName);
        if (fallback != null) return fallback;
        IClass<? super T> superclass = getSuperclass();
        if (superclass != null) {
            return superclass.getField(fieldName);
        }
        throw new NoSuchFieldException(fieldName);
    }

    @Override
    public IMethod getMethod(String methodName, IClass<?>... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        for (AOTMethod m : methods) {
            if (m.getName().equals(methodName) && Modifier.isPublic(m.getModifiers())
                    && parameterTypesMatch(m, parameterTypes)) {
                return m;
            }
        }
        // Try the live-class fallback BEFORE walking the superclass chain.
        // Class.forName(name).getMethod(...) already searches the entire
        // public-inheritance chain natively, and doesn't require an
        // IReflection to be configured (unlike getSuperclass() which would
        // recurse through the AOT registry). This makes shallow descriptors
        // (CoreInfrastructureSeed.synthesize) resolve inherited public
        // methods correctly without needing the full reflection wiring.
        AOTMethod fallback = liveFallback.publicMethod(methodName, parameterTypes);
        if (fallback != null) return fallback;
        // Final fallback: AOT superclass walk — relevant when the live class
        // isn't reachable but the AOT registry has a rich superclass descriptor.
        IClass<? super T> superclass = getSuperclass();
        if (superclass != null) {
            return superclass.getMethod(methodName, parameterTypes);
        }
        throw new NoSuchMethodException(methodName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IConstructor<T> getConstructor(IClass<?>... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        for (AOTConstructor<?> c : constructors) {
            if (Modifier.isPublic(c.getModifiers()) && constructorParameterTypesMatch(c, parameterTypes)) {
                return (IConstructor<T>) c;
            }
        }
        AOTConstructor<?> fallback = liveFallback.publicConstructor(parameterTypes);
        if (fallback != null) return (IConstructor<T>) fallback;
        throw new NoSuchMethodException(name + ".<init>");
    }

    // --- Record components ---

    @Override
    public IRecordComponent[] getRecordComponents() {
        if (!isRecord) return null;
        return new IRecordComponent[0];
    }

    // --- Nesting & enclosing ---

    @Override
    public IClass<?> getEnclosingClass() {
        return IClass.getClass(getTypeResolved().getEnclosingClass());
    }

    @Override
    public IClass<?> getDeclaringClass() {
        Class<?> dc = getTypeResolved().getDeclaringClass();
        return dc == null ? null : IClass.getClass(dc);
    }

    @Override
    public IMethod getEnclosingMethod() {
        return null;
    }

    @Override
    public IConstructor<?> getEnclosingConstructor() {
        return null;
    }

    @Override
    public IClass<?> getNestHost() {
        return IClass.getClass(getTypeResolved().getNestHost());
    }

    @Override
    public IClass<?>[] getNestMembers() {
        return Arrays.stream(getTypeResolved().getNestMembers())
                .map(IClass::getClass)
                .toArray(IClass<?>[]::new);
    }

    @Override
    public boolean isNestmateOf(IClass<?> c) {
        return getTypeResolved().isNestmateOf((Class<?>) c.getType());
    }

    // --- Inner classes ---

    @Override
    public IClass<?>[] getClasses() {
        return Arrays.stream(getTypeResolved().getClasses())
                .map(IClass::getClass)
                .toArray(IClass<?>[]::new);
    }

    @Override
    public IClass<?>[] getDeclaredClasses() {
        return Arrays.stream(getTypeResolved().getDeclaredClasses())
                .map(IClass::getClass)
                .toArray(IClass<?>[]::new);
    }

    // --- Sealed ---

    @Override
    public IClass<?>[] getPermittedSubclasses() {
        if (!isSealed) return null;
        return Arrays.stream(getTypeResolved().getPermittedSubclasses())
                .map(IClass::getClass)
                .toArray(IClass<?>[]::new);
    }

    // --- Enum ---

    @Override
    @SuppressWarnings("unchecked")
    public T[] getEnumConstants() {
        return (T[]) getTypeResolved().getEnumConstants();
    }

    // --- Annotations ---

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
    public AnnotatedType getAnnotatedSuperclass() {
        return getTypeResolved().getAnnotatedSuperclass();
    }

    @Override
    public AnnotatedType[] getAnnotatedInterfaces() {
        return getTypeResolved().getAnnotatedInterfaces();
    }

    // --- Cast ---

    @Override
    @SuppressWarnings("unchecked")
    public T cast(Object obj) {
        return (T) getTypeResolved().cast(obj);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> IClass<? extends U> asSubclass(IClass<U> clazz) {
        Class<? extends U> sub = getTypeResolved().asSubclass((Class<U>) clazz.getType());
        return (IClass<? extends U>) IClass.getClass(sub);
    }

    // --- Runtime info ---

    @Override
    public Package getPackage() {
        return getTypeResolved().getPackage();
    }

    @Override
    public Module getModule() {
        return getTypeResolved().getModule();
    }

    @Override
    public ClassLoader getClassLoader() {
        return getTypeResolved().getClassLoader();
    }

    @Override
    public Type getType() {
        return getTypeResolved();
    }

    @Override
    public IReflection reflection() {
        return IClass.getReflection();
    }

    // --- Lazy Class resolution ---

    @SuppressWarnings("unchecked")
    private Class<T> getTypeResolved() {
        Class<T> cached = resolvedClass;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedClass == null) {
                try {
                    // Array-aware: parses "byte[]" / "int[][]" / "java.lang.String[]"
                    // forms that the AOT processor emits. Plain Class.forName
                    // would NPE on these because Java-source array notation
                    // isn't valid for Class.forName (it wants JVM internal
                    // names like "[B" or descriptor "[Ljava.lang.String;").
                    resolvedClass = (Class<T>) AOTMethod.resolveRawClass(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot resolve class: " + name, e);
                }
            }
            return resolvedClass;
        }
    }

    // --- Helper methods ---

    private boolean parameterTypesMatch(AOTMethod m, IClass<?>[] parameterTypes) {
        IClass<?>[] mParams = m.getParameterTypes();
        if (mParams.length != parameterTypes.length) return false;
        for (int i = 0; i < mParams.length; i++) {
            if (!mParams[i].getName().equals(parameterTypes[i].getName())) return false;
        }
        return true;
    }

    private boolean constructorParameterTypesMatch(AOTConstructor<?> c, IClass<?>[] parameterTypes) {
        IClass<?>[] cParams = c.getParameterTypes();
        if (cParams.length != parameterTypes.length) return false;
        for (int i = 0; i < cParams.length; i++) {
            if (!cParams[i].getName().equals(parameterTypes[i].getName())) return false;
        }
        return true;
    }

    // --- Object overrides ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof AOTClass<?> other) return name.equals(other.name);
        if (obj instanceof IClass<?> other) return name.equals(other.getName());
        if (obj instanceof Class<?> other) return name.equals(other.getName());
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return (isInterface ? "interface " : (isEnum ? "enum " : "class ")) + name;
    }

}
