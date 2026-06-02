package com.garganttua.core.aot.reflection;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;

/**
 * On-demand synthesis of {@code AOT*} members from the live {@link Class} for
 * shallow {@link AOTClass} descriptors.
 *
 * <p>Shallow descriptors (produced by {@code CoreInfrastructureSeed.synthesize}
 * for types not run through the annotation processor) carry only identity +
 * flags + a synthesised no-arg constructor. When framework wiring requests a
 * method, field or non-trivial constructor, this collaborator loads the live
 * {@code Class<?>} via {@code Class.forName} and synthesises the {@code AOT*}
 * member on demand. Pure-AOT JVM mode works since {@code Class.forName} +
 * reflection are available; native-image consumers need the member in
 * reflect-config (handled by the {@code GarganttuaAotFeature} for seeded types).
 *
 * <p>One instance is owned per {@link AOTClass}. The bulk-array fallbacks
 * memoise their result and {@link #loadLiveClass()} memoises the resolved
 * {@code Class<?>}, so repeated calls during auto-detection no longer re-run
 * {@code Class.forName} + re-allocate {@code N×AOTMethod} per invocation. The
 * single-member fallbacks stay non-cached: each (name, parameter-types) tuple
 * would need its own cache key and the lookup pattern is one-shot per name.
 *
 * <p>Thread-safe: {@code loadLiveClass} double-checks under {@code synchronized};
 * the cache fields are {@code volatile}.
 */
final class AOTLiveClassFallback {

    private final String name;

    private volatile Class<?> liveClassCache;
    private volatile boolean liveClassResolved;
    private volatile IMethod[] cachedFallbackMethods;
    private volatile IField[] cachedFallbackFields;
    private volatile IConstructor<?>[] cachedFallbackConstructors;

    AOTLiveClassFallback(String name) {
        this.name = name;
    }

    private Class<?> loadLiveClass() {
        if (liveClassResolved) return liveClassCache;
        synchronized (this) {
            if (liveClassResolved) return liveClassCache;
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) cl = AOTLiveClassFallback.class.getClassLoader();
                liveClassCache = Class.forName(name, false, cl);
            } catch (ClassNotFoundException | LinkageError ignored) {
                liveClassCache = null;
            }
            liveClassResolved = true;
            return liveClassCache;
        }
    }

    private Class<?>[] toRawClasses(IClass<?>[] parameterTypes) {
        if (parameterTypes == null) return new Class<?>[0];
        Class<?>[] out = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            IClass<?> p = parameterTypes[i];
            if (p == null) return null;
            java.lang.reflect.Type t = p.getType();
            if (!(t instanceof Class<?> c)) return null;
            out[i] = c;
        }
        return out;
    }

    AOTMethod declaredMethod(String methodName, IClass<?>... parameterTypes) {
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        Class<?>[] raw = toRawClasses(parameterTypes);
        if (raw == null) return null;
        try {
            return AOTMethod.synthesizeFrom(live.getDeclaredMethod(methodName, raw));
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    AOTMethod publicMethod(String methodName, IClass<?>... parameterTypes) {
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        Class<?>[] raw = toRawClasses(parameterTypes);
        if (raw == null) return null;
        try {
            return AOTMethod.synthesizeFrom(live.getMethod(methodName, raw));
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    AOTField declaredField(String fieldName) {
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        try {
            return AOTField.synthesizeFrom(live.getDeclaredField(fieldName));
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    AOTField publicField(String fieldName) {
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        try {
            return AOTField.synthesizeFrom(live.getField(fieldName));
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    AOTConstructor<?> declaredConstructor(IClass<?>... parameterTypes) {
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        Class<?>[] raw = toRawClasses(parameterTypes);
        if (raw == null) return null;
        try {
            return AOTConstructor.synthesizeFrom(live.getDeclaredConstructor(raw));
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    AOTConstructor<?> publicConstructor(IClass<?>... parameterTypes) {
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        Class<?>[] raw = toRawClasses(parameterTypes);
        if (raw == null) return null;
        try {
            return AOTConstructor.synthesizeFrom(live.getConstructor(raw));
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    IMethod[] declaredMethods() {
        IMethod[] cached = cachedFallbackMethods;
        if (cached != null) return cached.length == 0 ? null : cached.clone();
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        try {
            java.lang.reflect.Method[] raw = live.getDeclaredMethods();
            IMethod[] out = new IMethod[raw.length];
            for (int i = 0; i < raw.length; i++) {
                out[i] = AOTMethod.synthesizeFrom(raw[i]);
            }
            cachedFallbackMethods = out;
            return out.clone();
        } catch (Throwable ignored) {
            cachedFallbackMethods = new IMethod[0];
            return null;
        }
    }

    IField[] declaredFields() {
        IField[] cached = cachedFallbackFields;
        if (cached != null) return cached.length == 0 ? null : cached.clone();
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        try {
            java.lang.reflect.Field[] raw = live.getDeclaredFields();
            IField[] out = new IField[raw.length];
            for (int i = 0; i < raw.length; i++) {
                out[i] = AOTField.synthesizeFrom(raw[i]);
            }
            cachedFallbackFields = out;
            return out.clone();
        } catch (Throwable ignored) {
            cachedFallbackFields = new IField[0];
            return null;
        }
    }

    IConstructor<?>[] declaredConstructors() {
        IConstructor<?>[] cached = cachedFallbackConstructors;
        if (cached != null) return cached.length == 0 ? null : cached.clone();
        Class<?> live = loadLiveClass();
        if (live == null) return null;
        try {
            java.lang.reflect.Constructor<?>[] raw = live.getDeclaredConstructors();
            IConstructor<?>[] out = new IConstructor<?>[raw.length];
            for (int i = 0; i < raw.length; i++) {
                out[i] = AOTConstructor.synthesizeFrom(raw[i]);
            }
            cachedFallbackConstructors = out;
            return out.clone();
        } catch (Throwable ignored) {
            cachedFallbackConstructors = new IConstructor<?>[0];
            return null;
        }
    }
}
