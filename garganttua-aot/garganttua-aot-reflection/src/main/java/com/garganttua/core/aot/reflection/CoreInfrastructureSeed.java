package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.dsl.IObservabilityBuilder;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.script.dsl.IScriptsBuilder;
import com.garganttua.core.workflow.dsl.IWorkflowsBuilder;

/**
 * Pre-populates {@link AOTRegistry} with descriptors for the framework's
 * "infrastructure" interfaces — the ones the framework's own classes
 * resolve via {@code IClass.getClass(...)} at static-init time (most
 * notably {@code Bootstrap.buildDependencies()} which needs
 * {@link IReflectionBuilder} and {@link IObservabilityBuilder} before any
 * user code runs).
 *
 * <p>Without this seed, an application running in AOT-only mode (no
 * {@code garganttua-runtime-reflection} on the classpath) crashes at
 * {@code new Bootstrap()} because no user-side AOT processor ever sees
 * these interfaces (annotation processors only see types compiled in the
 * current compilation unit, never types from JAR dependencies).
 *
 * <p>The seed is triggered by the static initialiser of
 * {@link AOTReflectionProvider} — the first time the SPI ServiceLoader
 * instantiates the provider, this class is loaded and its {@code <clinit>}
 * registers the descriptors.
 *
 * <p>Implementation note: building an {@link AOTClass} for an interface
 * does NOT require reflection in the GraalVM sense — {@code Class.getName()},
 * {@code .getSimpleName()}, {@code .getCanonicalName()}, {@code .getPackageName()},
 * {@code .getModifiers()} and {@code .getInterfaces()} are class-metadata
 * accessors that work even on a fully-AOT'ed JVM. Annotations on the
 * interface are intentionally NOT exposed here — the dep system only needs
 * the type identifier.
 *
 * @since 2.0.0-ALPHA02
 */
public final class CoreInfrastructureSeed {

    private static volatile boolean seeded = false;

    private CoreInfrastructureSeed() {
    }

    /**
     * Idempotent. Safe to call multiple times — only the first call writes
     * to the registry, subsequent calls are a no-op.
     */
    public static synchronized void bootstrap() {
        if (seeded) {
            return;
        }
        registerInterface(IReflectionBuilder.class);
        registerInterface(IObservabilityBuilder.class);
        registerInterface(IInjectionContextBuilder.class);
        registerInterface(IExpressionContextBuilder.class);
        registerInterface(IRuntimesBuilder.class);
        registerInterface(IScriptsBuilder.class);
        registerInterface(IWorkflowsBuilder.class);
        registerInterface(IConditionBuilder.class);
        registerInterface(IInjectableElementResolverBuilder.class);
        seeded = true;
    }

    private static <T> void registerInterface(Class<T> iface) {
        if (AOTRegistry.getInstance().contains(iface.getName())) {
            return;
        }
        Class<?>[] supers = iface.getInterfaces();
        String[] superNames = new String[supers.length];
        for (int i = 0; i < supers.length; i++) {
            superNames[i] = supers[i].getName();
        }
        AOTClass<T> descriptor = new AOTClass<>(
                iface.getName(),
                iface.getSimpleName(),
                iface.getCanonicalName(),
                iface.getPackageName(),
                iface.getModifiers() | Modifier.INTERFACE,
                null,                                // no superclass for interfaces
                superNames,
                new AOTField[0],
                new AOTMethod[0],
                new AOTConstructor<?>[0],
                new Annotation[0],
                true,   // isInterface
                false,  // isArray
                false,  // isPrimitive
                false,  // isAnnotation
                false,  // isEnum
                false,  // isRecord
                false,  // isSealed
                false,  // isHidden
                false,  // isMemberClass
                false,  // isLocalClass
                false,  // isAnonymousClass
                false   // isSynthetic
        );
        AOTRegistry.getInstance().register(iface.getName(), descriptor);
    }
}
