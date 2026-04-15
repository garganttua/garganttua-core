package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.garganttua.core.reflection.IAnnotatedType;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;

/**
 * AOT implementation of {@link IParameter}.
 *
 * <p>Stores pre-computed parameter metadata. The parameter type is resolved
 * lazily via {@link IClass#forName(String)} on first access.</p>
 */
public class AOTParameter implements IParameter {

    private final String name;
    private final String typeName;
    private final int modifiers;
    private final boolean namePresent;
    private final boolean implicit;
    private final boolean synthetic;
    private final boolean varArgs;
    private final Annotation[] annotations;

    // Lazy resolution
    private volatile IClass<?> resolvedType;

    public AOTParameter(String name, String typeName, int modifiers,
                        boolean namePresent, boolean implicit, boolean synthetic,
                        boolean varArgs, Annotation[] annotations) {
        this.name = name;
        this.typeName = typeName;
        this.modifiers = modifiers;
        this.namePresent = namePresent;
        this.implicit = implicit;
        this.synthetic = synthetic;
        this.varArgs = varArgs;
        this.annotations = annotations != null ? annotations.clone() : new Annotation[0];
    }

    @Override
    public boolean isNamePresent() {
        return namePresent;
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
    public IClass<?> getType() {
        IClass<?> cached = resolvedType;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedType == null) {
                try {
                    resolvedType = IClass.forName(typeName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot resolve parameter type: " + typeName, e);
                }
            }
            return resolvedType;
        }
    }

    @Override
    public Type getParameterizedType() {
        return (Type) getType().getType();
    }

    @Override
    public boolean isImplicit() {
        return implicit;
    }

    @Override
    public boolean isSynthetic() {
        return synthetic;
    }

    @Override
    public boolean isVarArgs() {
        return varArgs;
    }

    @Override
    public IAnnotatedType getAnnotatedType() {
        return null;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof AOTParameter other) {
            return name.equals(other.name) && typeName.equals(other.typeName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + typeName.hashCode();
    }

    @Override
    public String toString() {
        return typeName + " " + name;
    }
}
