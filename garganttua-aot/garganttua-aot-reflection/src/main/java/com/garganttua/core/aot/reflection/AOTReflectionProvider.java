package com.garganttua.core.aot.reflection;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

import jakarta.annotation.Priority;

/**
 * AOT implementation of {@link IReflectionProvider}.
 *
 * <h2>Resolution strategy</h2>
 * <ol>
 *   <li><strong>Registry hit</strong> — the type was pre-registered by either
 *       {@link CoreInfrastructureSeed}, an {@code IAOTInfrastructureSeed}
 *       extension seed, or a generated {@code AOTClass_*} self-registration.
 *       Full descriptor with member metadata is returned.</li>
 *   <li><strong>Fallback synthesis</strong> — the type is not registered but
 *       we have its {@code Class<?>} literal (or can {@code Class.forName} it).
 *       A <em>type-identity</em> descriptor is synthesized on the fly via
 *       {@link CoreInfrastructureSeed#synthesize(Class)} — name, modifiers,
 *       superclass, interfaces, JVM flags — and cached in the registry. No
 *       member metadata.</li>
 * </ol>
 *
 * <p>The fallback eliminates the entire class of "missing AOT descriptor"
 * cold-start failures for type-identity uses (parameter types, field types,
 * return types resolved by other {@code AOTClass_*} descriptors). Member
 * introspection ({@code getDeclaredMethods}, etc.) still requires either a
 * full annotation-processor-generated descriptor or an explicit seed.</p>
 *
 * <h2>Hybrid mode</h2>
 * <p>{@link #supports(Class)} returns true only for <em>actually registered</em>
 * (or intrinsic) types. In hybrid mode (AOT @20 + runtime-reflection @10), the
 * runtime provider keeps ownership of types AOT hasn't pre-registered — its
 * full reflection is preferred over our shallow synthesis. The fallback only
 * fires when AOT is the sole provider (pure-AOT / native-image consumers).</p>
 */
@Priority(20)
public class AOTReflectionProvider implements IReflectionProvider {

    static {
        // Seed AOTRegistry with descriptors for the framework's public
        // infrastructure interfaces — provides FULL descriptors (where they
        // matter for member introspection) ahead of any user code running.
        // After this, getClass() and forName() will fall back to type-identity
        // synthesis for anything not seeded.
        CoreInfrastructureSeed.bootstrap();
    }

    @Override
    public <T> IClass<T> getClass(Class<T> clazz) {
        java.util.Optional<IClass<T>> hit = AOTRegistry.getInstance().get(clazz.getName());
        if (hit.isPresent()) {
            return hit.get();
        }
        // Fallback: synthesize a type-identity descriptor from the class
        // literal. Cached so subsequent lookups skip the synth.
        IClass<T> synthesized = CoreInfrastructureSeed.synthesize(clazz);
        AOTRegistry.getInstance().register(clazz.getName(), synthesized);
        return synthesized;
    }

    @Override
    public <T> IClass<T> forName(String className) throws ClassNotFoundException {
        return forNameImpl(className, true, Thread.currentThread().getContextClassLoader());
    }

    @Override
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
        // First try intrinsic resolution (primitives, arrays, void) — handled
        // by AOTMethod.resolveRawClass without touching the JVM classloader.
        Class<?> raw;
        try {
            raw = AOTMethod.resolveRawClass(className);
        } catch (ClassNotFoundException intrinsicMiss) {
            // Then try the JVM classloader. If the class is reachable, we
            // synthesize a type-identity descriptor for it. Native-image
            // reachability requires the class to already be in reflect-config;
            // user @Reflected types are handled by the AOT Feature.
            try {
                raw = Class.forName(className, initialize, loader);
            } catch (ClassNotFoundException notReachable) {
                throw new ClassNotFoundException(
                        "AOT provider could not resolve: " + className, notReachable);
            }
        }
        IClass<T> synthesized = (IClass<T>) CoreInfrastructureSeed.synthesize(raw);
        AOTRegistry.getInstance().register(className, synthesized);
        return synthesized;
    }

    @Override
    public boolean supports(Class<?> type) {
        // Hybrid-mode contract: only claim types AOT actually owns. The
        // fallback in getClass/forName fires on direct invocation (pure-AOT
        // mode) but does NOT make this provider claim universal ownership.
        return AOTRegistry.getInstance().contains(type.getName()) || isIntrinsic(type);
    }

    /** Primitives, arrays, and void don't need compile-time AOT processing —
     *  the JVM exposes their metadata intrinsically. */
    private static boolean isIntrinsic(Class<?> type) {
        return type.isPrimitive() || type.isArray() || type == void.class;
    }
}
