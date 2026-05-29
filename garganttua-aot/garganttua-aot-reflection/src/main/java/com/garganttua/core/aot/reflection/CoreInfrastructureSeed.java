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
        // Framework infrastructure builder interfaces — resolved at static-init
        // time by the framework's own classes.
        registerInterface(IReflectionBuilder.class);
        registerInterface(IObservabilityBuilder.class);
        registerInterface(IInjectionContextBuilder.class);
        registerInterface(IExpressionContextBuilder.class);
        registerInterface(IRuntimesBuilder.class);
        registerInterface(IScriptsBuilder.class);
        registerInterface(IWorkflowsBuilder.class);
        registerInterface(IConditionBuilder.class);
        registerInterface(IInjectableElementResolverBuilder.class);
        // JDK collection interfaces used by framework builder return types
        // (e.g. RuntimesBuilder produces a Map<String, IRuntime<?,?>>, etc.).
        registerInterface(java.util.Map.class);
        registerInterface(java.util.List.class);
        registerInterface(java.util.Set.class);
        registerInterface(java.util.Collection.class);
        registerInterface(java.lang.Iterable.class);
        // JDK common types resolved as IClass at framework-level wiring.
        registerClass(java.lang.String.class);
        registerClass(java.lang.Object.class);
        registerClass(java.lang.Integer.class);
        registerClass(java.lang.Long.class);
        registerClass(java.lang.Boolean.class);
        registerClass(java.lang.Double.class);
        registerClass(java.lang.Float.class);
        registerClass(java.lang.Void.class);
        registerClass(java.util.Optional.class);
        registerClass(java.util.UUID.class);
        seeded = true;
    }

    private static <T> void registerInterface(Class<T> iface) {
        registerType(iface, true);
    }

    private static <T> void registerClass(Class<T> clazz) {
        registerType(clazz, false);
    }

    private static <T> void registerType(Class<T> type, boolean forceInterfaceFlag) {
        if (AOTRegistry.getInstance().contains(type.getName())) {
            return;
        }
        Class<?>[] supers = type.getInterfaces();
        String[] superNames = new String[supers.length];
        for (int i = 0; i < supers.length; i++) {
            superNames[i] = supers[i].getName();
        }
        Class<?> superclass = type.getSuperclass();
        AOTClass<T> descriptor = new AOTClass<>(
                type.getName(),
                type.getSimpleName(),
                type.getCanonicalName(),
                type.getPackageName(),
                forceInterfaceFlag ? type.getModifiers() | Modifier.INTERFACE : type.getModifiers(),
                superclass != null ? superclass.getName() : null,
                superNames,
                new AOTField[0],
                new AOTMethod[0],
                new AOTConstructor<?>[0],
                new Annotation[0],
                type.isInterface(),
                type.isArray(),
                type.isPrimitive(),
                type.isAnnotation(),
                type.isEnum(),
                type.isRecord(),
                type.isSealed(),
                type.isHidden(),
                type.isMemberClass(),
                type.isLocalClass(),
                type.isAnonymousClass(),
                type.isSynthetic()
        );
        AOTRegistry.getInstance().register(type.getName(), descriptor);
    }
}
