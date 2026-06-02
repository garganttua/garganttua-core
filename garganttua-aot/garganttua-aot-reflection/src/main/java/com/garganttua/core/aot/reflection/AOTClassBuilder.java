package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.aot.commons.IAOTClassBuilder;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for constructing {@link AOTClass} descriptors.
 *
 * <p>Extends {@link AbstractAutomaticBuilder} for auto-detection support.
 * When {@code autoDetect(true)} is called and the target class has a
 * {@link Reflected} annotation, the builder pre-configures itself from the
 * annotation attributes.</p>
 *
 * @param <T> the type being described
 */
public class AOTClassBuilder<T> extends AbstractAutomaticBuilder<IAOTClassBuilder<T>, IClass<T>>
        implements IAOTClassBuilder<T> {
    private static final Logger log = Logger.getLogger(AOTClassBuilder.class);

    private final IClass<T> targetClass;
    private final List<AOTField> fieldList = new ArrayList<>();
    private final List<AOTMethod> methodList = new ArrayList<>();
    private final List<AOTConstructor<?>> constructorList = new ArrayList<>();

    // Global flags
    private boolean queryAllDeclaredConstructors;
    private boolean queryAllPublicConstructors;
    private boolean queryAllDeclaredMethods;
    private boolean queryAllPublicMethods;
    private boolean allDeclaredFields;
    private boolean allPublicFields;
    private boolean allDeclaredClasses;

    /**
     * Creates a builder that describes the given target class.
     *
     * @param targetClass the class whose AOT descriptor is being assembled
     */
    public AOTClassBuilder(IClass<T> targetClass) {
        this.targetClass = targetClass;
    }

    // --- Field addition / removal ---

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> field(String fieldName) {
        try {
            IField field = targetClass.getDeclaredField(fieldName);
            return field(field);
        } catch (NoSuchFieldException e) {
            log.warn("Field not found: {} in {}", fieldName, targetClass.getName());
        }
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> field(IField field) {
        AOTField aotField = new AOTField(
                field.getName(),
                field.getDeclaringClass().getName(),
                field.getType().getName(),
                field.getModifiers(),
                field.getDeclaredAnnotations(),
                field.getGenericType()
        );
        fieldList.add(aotField);
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> fieldsAnnotatedWith(IClass<? extends Annotation> annotation) {
        IReflection reflection = IClass.getReflection();
        for (IField f : targetClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(annotation)) {
                field(f);
            }
        }
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> removeField(String fieldName) {
        Iterator<AOTField> it = fieldList.iterator();
        while (it.hasNext()) {
            if (it.next().getName().equals(fieldName)) {
                it.remove();
                break;
            }
        }
        return (IAOTClassBuilder<T>) this;
    }

    // --- Method addition / removal ---

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> method(String methodName, IClass<?>... parameterTypes) {
        try {
            IMethod method = targetClass.getDeclaredMethod(methodName, parameterTypes);
            return method(method);
        } catch (NoSuchMethodException e) {
            log.warn("Method not found: {} in {}", methodName, targetClass.getName());
        }
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> method(IMethod method) {
        IClass<?>[] paramTypes = method.getParameterTypes();
        String[] paramTypeNames = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypeNames[i] = paramTypes[i].getName();
        }

        IClass<?>[] exTypes = method.getExceptionTypes();
        String[] exTypeNames = new String[exTypes.length];
        for (int i = 0; i < exTypes.length; i++) {
            exTypeNames[i] = exTypes[i].getName();
        }

        String[] paramNames = Arrays.stream(method.getParameters())
                .map(p -> p.getName())
                .toArray(String[]::new);

        AOTMethod aotMethod = new AOTMethod(
                method.getName(),
                method.getDeclaringClass().getName(),
                method.getReturnType().getName(),
                paramTypeNames,
                paramNames,
                method.getModifiers(),
                method.getDeclaredAnnotations(),
                method.isBridge(),
                method.isDefault(),
                method.isVarArgs(),
                exTypeNames
        );
        methodList.add(aotMethod);
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> methodsAnnotatedWith(IClass<? extends Annotation> annotation) {
        for (IMethod m : targetClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotation)) {
                method(m);
            }
        }
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> removeMethod(String methodName, IClass<?>... parameterTypes) {
        Iterator<AOTMethod> it = methodList.iterator();
        while (it.hasNext()) {
            AOTMethod m = it.next();
            if (m.getName().equals(methodName) && m.getParameterCount() == parameterTypes.length) {
                IClass<?>[] mParams = m.getParameterTypes();
                boolean match = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!mParams[i].getName().equals(parameterTypes[i].getName())) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    it.remove();
                    break;
                }
            }
        }
        return (IAOTClassBuilder<T>) this;
    }

    // --- Constructor addition / removal ---

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> constructor(IClass<?>... parameterTypes) {
        try {
            IConstructor<T> ctor = targetClass.getDeclaredConstructor(parameterTypes);
            return constructor(ctor);
        } catch (NoSuchMethodException e) {
            log.warn("Constructor not found in {}", targetClass.getName());
        }
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> constructor(IConstructor<?> constructor) {
        IClass<?>[] paramTypes = constructor.getParameterTypes();
        String[] paramTypeNames = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypeNames[i] = paramTypes[i].getName();
        }

        IClass<?>[] exTypes = constructor.getExceptionTypes();
        String[] exTypeNames = new String[exTypes.length];
        for (int i = 0; i < exTypes.length; i++) {
            exTypeNames[i] = exTypes[i].getName();
        }

        String[] paramNames = Arrays.stream(constructor.getParameters())
                .map(p -> p.getName())
                .toArray(String[]::new);

        AOTConstructor<?> aotCtor = new AOTConstructor<>(
                constructor.getDeclaringClass().getName(),
                paramTypeNames,
                paramNames,
                constructor.getModifiers(),
                constructor.getDeclaredAnnotations(),
                constructor.isVarArgs(),
                exTypeNames
        );
        constructorList.add(aotCtor);
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> removeConstructor(IClass<?>... parameterTypes) {
        Iterator<AOTConstructor<?>> it = constructorList.iterator();
        while (it.hasNext()) {
            AOTConstructor<?> c = it.next();
            IClass<?>[] cParams = c.getParameterTypes();
            if (cParams.length == parameterTypes.length) {
                boolean match = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!cParams[i].getName().equals(parameterTypes[i].getName())) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    it.remove();
                    break;
                }
            }
        }
        return (IAOTClassBuilder<T>) this;
    }

    // --- Global flags ---

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> queryAllDeclaredConstructors(boolean value) {
        this.queryAllDeclaredConstructors = value;
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> queryAllPublicConstructors(boolean value) {
        this.queryAllPublicConstructors = value;
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> queryAllDeclaredMethods(boolean value) {
        this.queryAllDeclaredMethods = value;
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> queryAllPublicMethods(boolean value) {
        this.queryAllPublicMethods = value;
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> allDeclaredFields(boolean value) {
        this.allDeclaredFields = value;
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> allPublicFields(boolean value) {
        this.allPublicFields = value;
        return (IAOTClassBuilder<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAOTClassBuilder<T> allDeclaredClasses(boolean value) {
        this.allDeclaredClasses = value;
        return (IAOTClassBuilder<T>) this;
    }

    // --- AbstractAutomaticBuilder ---

    @Override
    protected void doAutoDetection() throws DslException {
        log.debug("Running AOT auto-detection for {}", targetClass.getName());

        // Read @Reflected annotation if present
        try {
            IClass<Reflected> reflectedClass = IClass.forName(Reflected.class.getName());
            Reflected reflected = targetClass.getAnnotation(reflectedClass);
            if (reflected != null) {
                queryAllDeclaredConstructors = reflected.queryAllDeclaredConstructors();
                queryAllPublicConstructors = reflected.queryAllPublicConstructors();
                queryAllDeclaredMethods = reflected.queryAllDeclaredMethods();
                queryAllPublicMethods = reflected.queryAllPublicMethods();
                allDeclaredFields = reflected.allDeclaredFields();
                allDeclaredClasses = reflected.allDeclaredClasses();
                log.debug("@Reflected annotation detected, flags applied");
            }
        } catch (ClassNotFoundException e) {
            log.warn("Could not resolve Reflected annotation class", e);
        }
    }

    @Override
    protected IClass<T> doBuild() throws DslException {
        log.debug("Building AOTClass for {}", targetClass.getName());
        applyGlobalMemberFlags();
        AOTClass<T> aotClass = assembleDescriptor();
        log.debug("AOTClass built for {} with {} fields, {} methods, {} constructors",
                targetClass.getName(), fieldList.size(), methodList.size(), constructorList.size());
        return aotClass;
    }

    /** Add every declared/public field, method and constructor requested via the global query flags. */
    private void applyGlobalMemberFlags() {
        if (allDeclaredFields) {
            for (IField f : targetClass.getDeclaredFields()) {
                if (fieldList.stream().noneMatch(af -> af.getName().equals(f.getName()))) {
                    field(f);
                }
            }
        }
        if (allPublicFields) {
            for (IField f : targetClass.getDeclaredFields()) {
                if (Modifier.isPublic(f.getModifiers())
                        && fieldList.stream().noneMatch(af -> af.getName().equals(f.getName()))) {
                    field(f);
                }
            }
        }
        if (queryAllDeclaredMethods) {
            for (IMethod m : targetClass.getDeclaredMethods()) {
                method(m);
            }
        }
        if (queryAllPublicMethods) {
            for (IMethod m : targetClass.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers())) {
                    method(m);
                }
            }
        }
        if (queryAllDeclaredConstructors) {
            for (IConstructor<?> c : targetClass.getDeclaredConstructors()) {
                constructor(c);
            }
        }
        if (queryAllPublicConstructors) {
            for (IConstructor<?> c : targetClass.getDeclaredConstructors()) {
                if (Modifier.isPublic(c.getModifiers())) {
                    constructor(c);
                }
            }
        }
    }

    /** Assemble the immutable AOTClass descriptor from the collected members + class metadata. */
    private AOTClass<T> assembleDescriptor() {
        IClass<? super T> superclass = targetClass.getSuperclass();
        String superclassName = superclass != null ? superclass.getName() : null;

        IClass<?>[] interfaces = targetClass.getInterfaces();
        String[] interfaceNames = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = interfaces[i].getName();
        }

        return new AOTClass<>(
                targetClass.getName(),
                targetClass.getSimpleName(),
                targetClass.getCanonicalName(),
                targetClass.getPackageName(),
                targetClass.getModifiers(),
                superclassName,
                interfaceNames,
                fieldList.toArray(new AOTField[0]),
                methodList.toArray(new AOTMethod[0]),
                constructorList.toArray(new AOTConstructor[0]),
                targetClass.getDeclaredAnnotations(),
                targetClass.isInterface(),
                targetClass.isArray(),
                targetClass.isPrimitive(),
                targetClass.isAnnotation(),
                targetClass.isEnum(),
                targetClass.isRecord(),
                targetClass.isSealed(),
                targetClass.isHidden(),
                targetClass.isMemberClass(),
                targetClass.isLocalClass(),
                targetClass.isAnonymousClass(),
                targetClass.isSynthetic()
        );
    }
}
