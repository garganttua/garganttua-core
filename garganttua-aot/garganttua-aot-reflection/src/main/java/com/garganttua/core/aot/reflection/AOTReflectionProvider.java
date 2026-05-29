package com.garganttua.core.aot.reflection;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

import jakarta.annotation.Priority;

/**
 * AOT implementation of {@link IReflectionProvider}.
 *
 * <p>Resolves classes from the {@link AOTRegistry} singleton. Classes that
 * are not in the registry but are <em>intrinsic</em> to the JVM (primitives,
 * arrays, void) get a synthesized minimal descriptor on the fly via
 * {@link CoreInfrastructureSeed#synthesize(Class)} — these types don't need
 * compile-time AOT processing because {@code Class.getName()},
 * {@code .getInterfaces()}, {@code .isArray()} etc. work without reflection
 * metadata.</p>
 */
@Priority(20)
public class AOTReflectionProvider implements IReflectionProvider {

    static {
        // Seed AOTRegistry with descriptors for the framework's public
        // infrastructure interfaces — Bootstrap's static init resolves
        // them via IClass.getClass(...) before user code runs, and the
        // consumer-side annotation processor can't see types from JAR
        // dependencies. Without this seed an AOT-only app crashes at
        // `new Bootstrap()`.
        CoreInfrastructureSeed.bootstrap();
    }

    @Override
    public <T> IClass<T> getClass(Class<T> clazz) {
        java.util.Optional<IClass<T>> hit = AOTRegistry.getInstance().get(clazz.getName());
        if (hit.isPresent()) {
            return hit.get();
        }
        if (isIntrinsic(clazz)) {
            IClass<T> synthesized = CoreInfrastructureSeed.synthesize(clazz);
            AOTRegistry.getInstance().register(clazz.getName(), synthesized);
            return synthesized;
        }
        throw new IllegalArgumentException(
                "No AOT descriptor registered for: " + clazz.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IClass<T> forName(String className) throws ClassNotFoundException {
        return forNameImpl(className, true, Thread.currentThread().getContextClassLoader());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader)
            throws ClassNotFoundException {
        return forNameImpl(className, initialize, loader);
    }

    @SuppressWarnings("unchecked")
    private <T> IClass<T> forNameImpl(String className, boolean initialize, ClassLoader loader)
            throws ClassNotFoundException {
        java.util.Optional<IClass<T>> hit = (java.util.Optional<IClass<T>>) (java.util.Optional<?>)
                AOTRegistry.getInstance().get(className);
        if (hit.isPresent()) {
            return hit.get();
        }
        // Try resolving as an intrinsic JVM type (primitives, arrays, void).
        Class<?> raw;
        try {
            raw = AOTMethod.resolveRawClass(className);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(
                    "No AOT descriptor registered for: " + className, e);
        }
        if (isIntrinsic(raw)) {
            IClass<T> synthesized = (IClass<T>) CoreInfrastructureSeed.synthesize(raw);
            AOTRegistry.getInstance().register(className, synthesized);
            return synthesized;
        }
        throw new ClassNotFoundException("No AOT descriptor registered for: " + className);
    }

    @Override
    public boolean supports(Class<?> type) {
        return AOTRegistry.getInstance().contains(type.getName()) || isIntrinsic(type);
    }

    /** Primitives, arrays, and void don't need compile-time AOT processing —
     *  the JVM exposes their metadata intrinsically. */
    private static boolean isIntrinsic(Class<?> type) {
        return type.isPrimitive() || type.isArray() || type == void.class;
    }
}
