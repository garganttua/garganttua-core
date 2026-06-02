package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

import com.garganttua.core.aot.commons.AOTRegistry;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.crypto.IHash;
import com.garganttua.core.crypto.IKey;
import com.garganttua.core.crypto.IKeyAlgorithm;
import com.garganttua.core.crypto.IKeyRealm;
import com.garganttua.core.crypto.IKeyRealmBuilder;
import com.garganttua.core.dsl.annotations.Scan;
import com.garganttua.core.dsl.dependency.DependsOn;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.annotations.BeanProvider;
import com.garganttua.core.injection.annotations.ChildContext;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.PropertyProvider;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.FieldMappingRules;
import com.garganttua.core.mapper.annotations.MappingIgnore;
import com.garganttua.core.mapper.annotations.ObjectMappingRule;
import com.garganttua.core.mapper.annotations.ObjectMappingRules;
import com.garganttua.core.mutex.annotations.Mutex;
import com.garganttua.core.mutex.annotations.MutexFactory;
import com.garganttua.core.reflection.annotations.IAnnotationIndex;
import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.ReflectedBuilder;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Condition;
import com.garganttua.core.runtime.annotations.Context;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Step;
import com.garganttua.core.runtime.annotations.Steps;
import com.garganttua.core.runtime.annotations.Synchronized;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.runtime.annotations.Variables;
import com.garganttua.core.script.annotations.ScriptDefinition;
import com.garganttua.core.workflow.annotations.WorkflowDefinition;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IBeanQuery;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.IContextualBeanSupplier;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IInjectionContextSupply;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.dsl.IObservabilityBuilder;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.runtime.IDomainRuntime;
import com.garganttua.core.runtime.IEventRuntime;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeExecutor;
import com.garganttua.core.runtime.IRuntimeResult;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.IRuntimeStepFallbackBinder;
import com.garganttua.core.runtime.IRuntimeStepMethodBinder;
import com.garganttua.core.runtime.IRuntimeStepOnException;
import com.garganttua.core.runtime.IRuntimeStepPipe;
import com.garganttua.core.runtime.IRuntimes;
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
        // Full injection.* public surface — DI infrastructure interfaces
        // routinely resolved by framework wiring at static-init time.
        registerInterface(IBeanFactory.class);
        registerInterface(IBeanProvider.class);
        registerInterface(IBeanQuery.class);
        registerInterface(IBeanQueryBuilder.class);
        registerInterface(IBeanSupplier.class);
        registerInterface(IContextualBeanSupplier.class);
        registerInterface(IElementResolver.class);
        registerInterface(IInjectableElementResolver.class);
        registerInterface(IInjectionChildContextFactory.class);
        registerInterface(IInjectionContext.class);
        registerInterface(IInjectionContextSupply.class);
        registerInterface(IPropertyProvider.class);
        registerInterface(IPropertySupplier.class);
        // Full runtime.* public surface — workflow-engine interfaces referenced
        // by user @Reflected runtime definitions, fallback binders, etc.
        registerInterface(IRuntime.class);
        registerInterface(IRuntimes.class);
        registerInterface(IRuntimeContext.class);
        registerInterface(IRuntimeExecutor.class);
        registerInterface(IRuntimeResult.class);
        registerInterface(IRuntimeStep.class);
        registerInterface(IRuntimeStepCatch.class);
        registerInterface(IRuntimeStepFallbackBinder.class);
        registerInterface(IRuntimeStepMethodBinder.class);
        registerInterface(IRuntimeStepOnException.class);
        registerInterface(IRuntimeStepPipe.class);
        registerInterface(IDomainRuntime.class);
        registerInterface(IEventRuntime.class);
        // Crypto API surface (commons-level) — user code resolves IKey at
        // static-init time for signing/encrypting fields, plus the sibling
        // interfaces of the same realm.
        registerInterface(IKey.class);
        registerInterface(IHash.class);
        registerInterface(IKeyAlgorithm.class);
        registerInterface(IKeyRealm.class);
        registerInterface(IKeyRealmBuilder.class);
        // Framework-public annotation surface (commons) — user-side @Reflected
        // descriptors reference these by class literal when materialising
        // their declared-annotation arrays.
        registerClass(Expression.class);
        registerClass(Reflected.class);
        registerClass(Indexed.class);
        registerClass(ReflectedBuilder.class);
        registerClass(BeanProvider.class);
        registerClass(ChildContext.class);
        registerClass(Fixed.class);
        registerClass(Null.class);
        registerClass(Property.class);
        registerClass(PropertyProvider.class);
        registerClass(Prototype.class);
        registerClass(Provider.class);
        registerClass(Resolver.class);
        // Reflection-side helper interface, surfaces in some indexed-discovery
        // paths.
        registerInterface(IAnnotationIndex.class);
        // Jakarta nullability markers — referenced by user @Reflected classes
        // as parameter / field / method annotations.
        registerClass(jakarta.annotation.Nullable.class);
        registerClass(jakarta.annotation.Nonnull.class);
        // JSR-330 (javax.inject) — DI annotation surface. The framework
        // indexes these by default; user code uses them on constructors,
        // fields, methods, and as meta-annotations for custom qualifiers.
        registerClass(javax.inject.Inject.class);
        registerClass(javax.inject.Named.class);
        registerClass(javax.inject.Qualifier.class);
        registerClass(javax.inject.Scope.class);
        registerClass(javax.inject.Singleton.class);
        registerInterface(javax.inject.Provider.class);
        // Bootstrap / DSL / dependency annotations
        registerClass(Bootstrap.class);
        registerClass(Scan.class);
        registerClass(DependsOn.class);
        // Mapper annotations — user DTOs frequently carry these.
        registerClass(FieldMappingRule.class);
        registerClass(FieldMappingRules.class);
        registerClass(MappingIgnore.class);
        registerClass(ObjectMappingRule.class);
        registerClass(ObjectMappingRules.class);
        // Mutex annotations
        registerClass(Mutex.class);
        registerClass(MutexFactory.class);
        // Runtime annotations — full workflow-engine annotation set.
        registerClass(Catch.class);
        registerClass(Code.class);
        registerClass(Condition.class);
        registerClass(Context.class);
        registerClass(com.garganttua.core.runtime.annotations.Exception.class);
        registerClass(ExceptionMessage.class);
        registerClass(FallBack.class);
        registerClass(Input.class);
        registerClass(OnException.class);
        registerClass(Operation.class);
        registerClass(Output.class);
        registerClass(RuntimeDefinition.class);
        registerClass(Step.class);
        registerClass(Steps.class);
        registerClass(Synchronized.class);
        registerClass(Variable.class);
        registerClass(Variables.class);
        // Script / Workflow definition markers
        registerClass(ScriptDefinition.class);
        registerClass(WorkflowDefinition.class);
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
        // Primitive wrapper completion
        registerClass(java.lang.Character.class);
        registerClass(java.lang.Short.class);
        registerClass(java.lang.Byte.class);
        // JDK number — domain-model essentials
        registerClass(java.math.BigDecimal.class);
        registerClass(java.math.BigInteger.class);
        // JDK time — user DTOs and entities pervasively use these
        registerClass(java.time.Instant.class);
        registerClass(java.time.LocalDate.class);
        registerClass(java.time.LocalDateTime.class);
        registerClass(java.time.LocalTime.class);
        registerClass(java.time.OffsetDateTime.class);
        registerClass(java.time.ZonedDateTime.class);
        registerClass(java.time.Duration.class);
        registerClass(java.time.Period.class);
        // JDK util
        registerClass(java.util.Date.class);
        registerClass(java.util.Locale.class);
        registerClass(java.util.TimeZone.class);
        // JDK collection interface completion
        registerInterface(java.util.Queue.class);
        registerInterface(java.util.Deque.class);
        registerInterface(java.util.SortedSet.class);
        registerInterface(java.util.NavigableSet.class);
        registerInterface(java.util.SortedMap.class);
        registerInterface(java.util.NavigableMap.class);
        // JDK collection implementations — user DTOs frequently declare these
        // as concrete types (List<X> field with new ArrayList<>() default, etc.).
        registerClass(java.util.ArrayList.class);
        registerClass(java.util.LinkedList.class);
        registerClass(java.util.HashMap.class);
        registerClass(java.util.LinkedHashMap.class);
        registerClass(java.util.TreeMap.class);
        registerClass(java.util.HashSet.class);
        registerClass(java.util.LinkedHashSet.class);
        registerClass(java.util.TreeSet.class);
        registerClass(java.util.ArrayDeque.class);
        // JDK meta-annotation surface — IAnnotatedElement.getDeclaredAnnotationsByType
        // walks @Repeatable to find container annotations.
        registerClass(java.lang.annotation.Repeatable.class);
        // Discover higher-layer-framework seeds (garganttua-api, -events, …)
        // BEFORE the user-generated AOTClass_* descriptors load — those
        // descriptors may reference framework-public types whose descriptors
        // come from these seeds.
        GeneratedDescriptorLoader.runExtensionSeeds();
        // Force-load every AOTClass_* generated by the annotation processor so
        // their static initialisers self-register into AOTRegistry.
        GeneratedDescriptorLoader.loadGeneratedDescriptors();
        seeded = true;
    }


    // Package-private: invoked by GeneratedDescriptorLoader.SeedContext to adapt
    // extension seeds to the IAOTSeedContext contract.
    static <T> void registerInterface(Class<T> iface) {
        registerType(iface, true);
    }

    static <T> void registerClass(Class<T> clazz) {
        registerType(clazz, false);
    }

    private static <T> void registerType(Class<T> type, boolean forceInterfaceFlag) {
        if (AOTRegistry.getInstance().contains(type.getName())) {
            return;
        }
        AOTRegistry.getInstance().register(type.getName(), synthesizeWithFlag(type, forceInterfaceFlag));
    }

    /**
     * Builds a minimal {@link AOTClass} descriptor for {@code type} using only
     * class-metadata accessors (no reflection in the GraalVM sense). Public
     * entry point used by {@code AOTReflectionProvider} to synthesize
     * intrinsic-JVM-type descriptors on the fly (primitives, arrays, void)
     * that the consumer-side annotation processor doesn't (and shouldn't)
     * generate descriptors for.
     */
    public static <T> AOTClass<T> synthesize(Class<T> type) {
        return synthesizeWithFlag(type, false);
    }

    private static <T> AOTClass<T> synthesizeWithFlag(Class<T> type, boolean forceInterfaceFlag) {
        Class<?>[] supers = type.getInterfaces();
        String[] superNames = new String[supers.length];
        for (int i = 0; i < supers.length; i++) {
            superNames[i] = supers[i].getName();
        }
        Class<?> superclass = type.getSuperclass();
        // Defensive: primitives / anonymous / local types can return null for
        // some of these accessors. AOTClass treats them as non-null strings.
        String canonicalName = type.getCanonicalName() != null ? type.getCanonicalName() : type.getName();
        String packageName = type.getPackageName() != null ? type.getPackageName() : "";
        AOTConstructor<?>[] constructors = synthesizeNoArgConstructor(type);
        Annotation[] annotations = safeAnnotations(type);
        return new AOTClass<>(
                type.getName(),
                type.getSimpleName(),
                canonicalName,
                packageName,
                forceInterfaceFlag ? type.getModifiers() | Modifier.INTERFACE : type.getModifiers(),
                superclass != null ? superclass.getName() : null,
                superNames,
                new AOTField[0],
                new AOTMethod[0],
                constructors,
                annotations,
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
    }

    /**
     * Synthesise the no-arg constructor descriptor when one exists — covers the
     * dominant "framework instantiates the type by reflection via no-arg ctor"
     * pattern (factories annotated @ChildContext, @MutexFactory, …). Empty array
     * when none / for interfaces / primitives / arrays / void.
     */
    private static <T> AOTConstructor<?>[] synthesizeNoArgConstructor(Class<T> type) {
        try {
            java.lang.reflect.Constructor<T> noArg = type.getDeclaredConstructor();
            return new AOTConstructor<?>[] {
                    new AOTConstructor<>(type.getName(), new String[0], new String[0],
                            noArg.getModifiers(), new Annotation[0], false, new String[0])
            };
        } catch (NoSuchMethodException ignored) {
            return new AOTConstructor<?>[0];
        } catch (Throwable ignored) {
            return new AOTConstructor<?>[0];
        }
    }

    /**
     * Class-level annotations from the live class (so framework wiring that reads
     * e.g. @Resolver sees them); empty array if introspection fails.
     */
    private static Annotation[] safeAnnotations(Class<?> type) {
        try {
            return type.getAnnotations();
        } catch (Throwable ignored) {
            return new Annotation[0];
        }
    }
}
