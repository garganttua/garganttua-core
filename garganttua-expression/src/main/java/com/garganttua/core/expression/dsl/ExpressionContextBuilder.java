package com.garganttua.core.expression.dsl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.context.IExpressionNodeFactory;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FutureSupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;
import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

import java.lang.reflect.Modifier;

import jakarta.annotation.Nullable;

/**
 * Builder for constructing ExpressionContext instances with fluent API.
 *
 * <p>
 * {@code ExpressionBuilder} implements the DSL builder pattern for creating
 * {@link IExpressionContext} objects. It extends
 * {@link AbstractAutomaticDependentBuilder} to
 * provide automatic configuration detection and package scanning capabilities.
 * </p>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * IExpressionContext context = ExpressionBuilder
 *         .builder()
 *         .withExpressionNode(Calculator.class, Integer.class)
 *         .method("add")
 *         .up()
 *         .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Bootstrap
@Reflected
public class ExpressionContextBuilder
        extends AbstractAutomaticDependentBuilder<IExpressionContextBuilder, IExpressionContext>
        implements IExpressionContextBuilder {
    private static final Logger log = Logger.getLogger(ExpressionContextBuilder.class);

    private static final String SOURCE_EXPLICIT = "explicit";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";

    /** Framework's own built-in expression / script / runtime / etc. function
     *  packages — always scanned so the framework's built-in @Expression
     *  methods (literal wrappers like {@code string}, script ops like
     *  {@code include} and {@code script_variable}, runtime accessors,
     *  conditions, …) are registered regardless of the consumer's package
     *  configuration. Without these, a consumer that only declares its own
     *  app packages parses fine until the first call to a framework built-in
     *  function ({@code include("foo.gs")}, {@code string("x")}, …) crashes
     *  with "Function not found" / "Undefined function".
     *
     *  Each package corresponds to one of the framework modules' Functions /
     *  Expressions classes (see the @Reflected sweep that covers them all).
     */
    private static final Set<String> BUILTIN_FUNCTIONS_PACKAGES = Set.of(
            "com.garganttua.core.expression.functions",
            "com.garganttua.core.script.functions",
            "com.garganttua.core.runtime.functions",
            "com.garganttua.core.injection.functions",
            "com.garganttua.core.injection.context.beans",
            "com.garganttua.core.mutex.functions",
            "com.garganttua.core.console",
            "com.garganttua.core.observability",
            "com.garganttua.core.condition",
            // Redis binding — harmless if the binding module isn't on classpath
            "com.garganttua.core.mutex.redis.functions"
    );

    private final Set<String> packages = new HashSet<>(BUILTIN_FUNCTIONS_PACKAGES);
    private final Set<IExpressionMethodBinderBuilder<?>> explicitNodes = new HashSet<>();
    private final Set<IExpressionMethodBinderBuilder<?>> autoDetectedNodes = new HashSet<>();
    private final MultiSourceCollector<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeCollector;
    private Set<IBuilderObserver<IExpressionContextBuilder, IExpressionContext>> observers = new HashSet<>();

    private static Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> buildNodeMap(
            Set<IExpressionMethodBinderBuilder<?>> builders) {
        Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> result = new HashMap<>();
        for (IExpressionMethodBinderBuilder<?> builder : builders) {
            IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = builder.build();
            result.put(factory.key(), factory);
        }
        return result;
    }

    protected ExpressionContextBuilder() {
        super(Set.of(DependencySpec.use(IClass.getClass(IInjectionContextBuilder.class))));
        log.trace("Entering ExpressionBuilder constructor");

        this.nodeCollector = new MultiSourceCollector<>();
        nodeCollector.source(nodeSetSupplier(explicitNodes), 0, SOURCE_EXPLICIT);
        nodeCollector.source(nodeSetSupplier(autoDetectedNodes), 1, SOURCE_AUTO_DETECTED);

        log.trace("Exiting ExpressionBuilder constructor");
    }

    @SuppressWarnings("unchecked")
    private ISupplier<Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>>> nodeSetSupplier(
            Set<IExpressionMethodBinderBuilder<?>> nodeSet) {
        return new ISupplier<>() {
            @Override
            public Optional<Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>>> supply() throws SupplyException {
                return Optional.of(buildNodeMap(nodeSet));
            }

            @Override
            public Type getSuppliedType() {
                return Map.class;
            }

            @Override
            public IClass<Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>>> getSuppliedClass() {
                return (IClass<Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>>>) (IClass<?>) IClass.getClass(Map.class);
            }
        };
    }

    /**
     * Creates a new ExpressionBuilder.
     *
     * @return a new ExpressionBuilder instance
     */
    public static ExpressionContextBuilder builder() {
        log.trace("Creating new ExpressionBuilder");
        return new ExpressionContextBuilder();
    }

    @Override
    public IExpressionContextBuilder observer(IBuilderObserver<IExpressionContextBuilder, IExpressionContext> observer) {
        log.trace("Entering observer(observer={})", observer);
        Objects.requireNonNull(observer, "Observer cannot be null");

        this.observers.add(observer);
        log.debug("Added observer: {}", observer);

        // If context is already built, notify the observer immediately
        if (this.built != null) {
            observer.handle(this.built);
            log.debug("Context already built, immediately notified observer: {}", observer);
        }

        log.trace("Exiting observer");
        return this;
    }

    private void notifyObservers(IExpressionContext built) {
        log.trace("Entering notifyObserver(built={})", built);
        this.observers.parallelStream().forEach(observer -> {
            observer.handle(built);
            log.debug("Notified observer: {}", observer);
        });
        log.trace("Exiting notifyObserver");
    }

    @Override
    public <T> IExpressionMethodBinderBuilder<T> expression(
            ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier, IClass<T> supplied) {
        log.debug("Creating ExpressionMethodBinderBuilder for methodOwnerSupplier={}, supplied={}",
                methodOwnerSupplier, supplied);
        Objects.requireNonNull(methodOwnerSupplier, "Method owner supplier cannot be null");
        Objects.requireNonNull(supplied, "Supplied type cannot be null");
        IExpressionMethodBinderBuilder<T> expressionNodeMethodBinderBuilder = new ExpressionNodeFactoryBuilder<>(this,
                methodOwnerSupplier, supplied);
        this.explicitNodes.add(expressionNodeMethodBinderBuilder);
        return expressionNodeMethodBinderBuilder;
    }

    @Override
    public IExpressionContextBuilder withPackage(String packageName) {
        log.debug("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IExpressionContextBuilder withPackages(String[] packageNames) {
        log.debug("Adding {} packages", packageNames.length);
        Objects.requireNonNull(packageNames, "Package names cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected IExpressionContext doBuild() throws DslException {
        CompletableFuture<ExpressionContext> futur = new CompletableFuture<>();
        try {
            IClass<ExpressionContext> ecClass = IClass.getClass(ExpressionContext.class);
            IClass<String> stringClass = IClass.getClass(String.class);
            this.expression(new FutureSupplierBuilder<>(futur, ecClass), stringClass)
                    .method(ecClass.getMethod("man")).withDescription("the description");
            this.expression(new FutureSupplierBuilder<>(futur, ecClass), stringClass)
                    .method(ecClass.getMethod("man", IClass.getClass(int.class))).withDescription("the description");
            this.expression(new FutureSupplierBuilder<>(futur, ecClass), stringClass)
                    .method(ecClass.getMethod("man", IClass.getClass(String.class))).withDescription("the description");
            // Literal-wrap functions — registered explicitly because the parser
            // depends on them for EVERY literal token. If the consumer's package
            // configuration doesn't reach the framework's expression.functions
            // package, auto-detection alone can miss them; the explicit
            // registration here guarantees they are always present. autoDetect
            // (true) makes the per-builder doAutoDetection read the @Expression
            // annotation for name + description.
            registerLiteralWrappers();
            // Belt and suspenders: enumerate every @Expression static method
            // on the framework's Functions classes and register them
            // explicitly. This bypasses the package scan entirely, so a
            // misconfigured consumer can never miss a framework built-in.
            // Classes are resolved via Class.forName; missing binding modules
            // (e.g. mutex-redis when Redis isn't on classpath) are silently
            // ignored.
            //
            // Why kept: empirically, removing this drops 15 functions in a
            // consumer's pure-AOT fat-jar (238 → 223) even though every
            // FRAMEWORK_FUNCTION_CLASSES package is already in the
            // BUILTIN_FUNCTIONS_PACKAGES set — the package-scan path
            // depends on the shade-plugin AppendingTransformer covering
            // every index file, and a single missed transformer silently
            // drops those entries. The reflection-based scan is the only
            // guarantee that survives a consumer's mis-configured shade.
            registerAllFrameworkBuiltinsByReflection();
        } catch (DslException | NoSuchMethodException | SecurityException e) {
            throw new DslException("Failed to register built-in expression nodes", e);
        }

        Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> mergedNodes = this.nodeCollector.build();
        Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> builtNodes = new HashSet<>(mergedNodes.values());

        ExpressionContext context = new ExpressionContext(builtNodes);
        futur.complete(context);

        this.notifyObservers(context);

        return context;
    }

    /**
     * Explicitly registers every literal-wrap function from
     * {@code com.garganttua.core.expression.functions.Expressions}. These are
     * the workhorses the parser invokes for EVERY literal token (string, int,
     * double, …) so without them the simplest script can't parse. Each is
     * registered with {@code autoDetect(true)} so the per-builder
     * doAutoDetection picks up the {@code @Expression} annotation's name and
     * description.
     */
    private void registerLiteralWrappers() throws DslException, NoSuchMethodException, SecurityException {
        IClass<com.garganttua.core.expression.functions.Expressions> exprCls =
                IClass.getClass(com.garganttua.core.expression.functions.Expressions.class);
        IClass<String> strCls = IClass.getClass(String.class);
        IClass<Object> objCls = IClass.getClass(Object.class);
        registerStaticBuiltin(exprCls, "string",        objCls, IClass.getClass(String.class));
        registerStaticBuiltin(exprCls, "integer",       strCls, IClass.getClass(int.class));
        registerStaticBuiltin(exprCls, "longnumber",    strCls, IClass.getClass(long.class));
        registerStaticBuiltin(exprCls, "doublenumber",  strCls, IClass.getClass(double.class));
        registerStaticBuiltin(exprCls, "floatnumber",   strCls, IClass.getClass(float.class));
        registerStaticBuiltin(exprCls, "booleanValue",  strCls, IClass.getClass(boolean.class));
        registerStaticBuiltin(exprCls, "byteValue",     strCls, IClass.getClass(byte.class));
        registerStaticBuiltin(exprCls, "shortNumber",   strCls, IClass.getClass(short.class));
        registerStaticBuiltin(exprCls, "character",     strCls, IClass.getClass(char.class));
    }

    private void registerStaticBuiltin(
            IClass<com.garganttua.core.expression.functions.Expressions> ownerCls,
            String methodName,
            IClass<?> paramType,
            IClass<?> returnType)
            throws DslException, NoSuchMethodException, SecurityException {
        this.expression(new NullSupplierBuilder<>(ownerCls), (IClass) returnType)
                .method(ownerCls.getMethod(methodName, paramType))
                .autoDetect(true);
    }

    /**
     * Framework function classes whose every @Expression-annotated static
     * method is registered directly by FQN. The package-scan path is the
     * "soft" version of this; this is the hard guarantee that all framework
     * built-ins are always present. Binding modules that aren't on the
     * classpath are silently skipped.
     */
    private static final String[] FRAMEWORK_FUNCTION_CLASSES = {
            // expression
            "com.garganttua.core.expression.functions.Expressions",
            // script
            "com.garganttua.core.script.functions.ScriptFunctions",
            "com.garganttua.core.script.functions.LogFunctions",
            "com.garganttua.core.script.functions.ControlFlowFunctions",
            // runtime
            "com.garganttua.core.runtime.functions.RuntimeFunctions",
            // injection
            "com.garganttua.core.injection.functions.InjectionFunctions",
            "com.garganttua.core.injection.context.beans.Beans",
            // mutex (+ binding-conditional redis)
            "com.garganttua.core.mutex.functions.MutexFunctions",
            "com.garganttua.core.mutex.redis.functions.RedisMutexFunctions",
            // console & observability
            "com.garganttua.core.console.ConsoleFunctions",
            "com.garganttua.core.observability.ObservabilityExpressions",
            // conditions — and / or / nor / nand / xor / not / null / notNull /
            // equals / notEquals / greater / greaterOrEquals / lower / lowerOrEquals
            "com.garganttua.core.condition.AndCondition",
            "com.garganttua.core.condition.OrCondition",
            "com.garganttua.core.condition.NorCondition",
            "com.garganttua.core.condition.NandCondition",
            "com.garganttua.core.condition.XorCondition",
            "com.garganttua.core.condition.NullCondition",
            "com.garganttua.core.condition.NotNullCondition",
            "com.garganttua.core.condition.EqualsCondition",
            "com.garganttua.core.condition.NotEqualsCondition",
            "com.garganttua.core.condition.GreaterCondition",
            "com.garganttua.core.condition.GreaterOrEqualsCondition",
            "com.garganttua.core.condition.LowerCondition",
            "com.garganttua.core.condition.LowerOrEqualsCondition"
    };

    @SuppressWarnings("unchecked")
    private void registerAllFrameworkBuiltinsByReflection() {
        IClass<? extends java.lang.annotation.Annotation> exprAnnoCls =
                (IClass<? extends java.lang.annotation.Annotation>) (IClass<?>)
                        IClass.getClass(com.garganttua.core.expression.annotations.Expression.class);
        for (String fqn : FRAMEWORK_FUNCTION_CLASSES) {
            Class<?> cls;
            try {
                cls = Class.forName(fqn, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException | LinkageError missing) {
                // Binding module not on classpath — fine.
                continue;
            }
            IClass<?> ownerCls = IClass.getClass(cls);
            for (com.garganttua.core.reflection.IMethod m : ownerCls.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                if (m.getAnnotation(exprAnnoCls) == null) {
                    continue;
                }
                try {
                    this.expression(new NullSupplierBuilder<>(ownerCls),
                                    (IClass) m.getReturnType())
                            .method(m)
                            .autoDetect(true);
                } catch (DslException ignored) {
                    // One bad function should not poison the whole context.
                }
            }
        }
    }

    /**
     * Automatically detects and registers methods annotated with @Expression.
     *
     * <p>
     * This method scans the configured packages for methods with the
     * {@code @Expression} annotation
     * and automatically creates expression node factories for them. It includes
     * signature-based
     * deduplication to handle cases where the scanner might return multiple Method
     * instances for
     * the same underlying method.
     * </p>
     *
     * <p>
     * The deduplication process ensures that:
     * </p>
     * <ul>
     * <li>Each unique method signature (class + method name + parameter types) is
     * registered only once</li>
     * <li>Overloaded methods with different signatures are all registered
     * correctly</li>
     * <li>Duplicate Method objects pointing to the same method are filtered
     * out</li>
     * </ul>
     *
     * @throws DslException if the builder is not authorized to build (missing
     *                      injection context)
     */
    @Override
    protected void doAutoDetection() throws DslException {
        // Synchronize packages from InjectionContextBuilder before scanning
        synchronizePackagesFromContext();

        IClass<Expression> expressionAnnotation = IClass.getClass(Expression.class);
        List<IMethod> expressions = new ArrayList<>();
        this.packages
                .forEach(p -> expressions.addAll(IClass.getReflection().getMethodsWithAnnotation(p, expressionAnnotation)));

        // Deduplicate methods by signature (declaring class + method name + parameter
        // types)
        // because distinct() only works with object identity, not method equivalence
        Map<String, IMethod> uniqueMethods = new LinkedHashMap<>();
        int duplicateCount = 0;
        for (IMethod m : expressions) {
            String signature = buildMethodSignature(m);
            if (uniqueMethods.putIfAbsent(signature, m) != null) {
                duplicateCount++;
            }
        }

        log.debug(
                "Found {} total methods with @Expression, {} unique after deduplication ({} duplicates removed)",
                expressions.size(), uniqueMethods.size(), duplicateCount);

        if (uniqueMethods.isEmpty() && !this.packages.isEmpty()) {
            log.warn(
                    "No @Expression methods found in packages {} — "
                    + "check that IClass.getReflection() has a scanner configured "
                    + "(e.g., ReflectionsAnnotationScanner). "
                    + "This will cause parsing failures for integer/boolean/string literals.",
                    this.packages);
        }

        // Create factories for unique methods only — add to auto-detected source
        // Static methods use NullSupplierBuilder (no bean instance needed),
        // non-static methods use BeanSupplierBuilder (requires bean registration)
        uniqueMethods.values()
                .forEach(m -> {
                    ISupplierBuilder<?, ?> supplier = Modifier.isStatic(m.getModifiers())
                            ? new NullSupplierBuilder<>(m.getDeclaringClass())
                            : new BeanSupplierBuilder<>(m.getDeclaringClass());
                    ExpressionNodeFactoryBuilder<?> builder = new ExpressionNodeFactoryBuilder<>(this,
                            supplier,
                            (IClass) m.getReturnType());
                    builder.method(m).autoDetect(true);
                    this.autoDetectedNodes.add(builder);
                });
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        log.trace("Entering doAutoDetectionWithDependency() with dependency: {}", dependency);
        // No dependency-based auto-detection needed
        log.trace("Exiting doAutoDetectionWithDependency() method");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        log.trace("Entering doPreBuildWithDependency() with dependency: {}", dependency);
        // Nothing to do in pre-build phase
        log.trace("Exiting doPreBuildWithDependency() method");
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        log.trace("Entering doPostBuildWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            log.debug("Registering IExpressionContext as bean in InjectionContext");
            BeanReference<IExpressionContext> beanRef = new BeanReference<>(
                    IClass.getClass(IExpressionContext.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.empty(),
                    Set.of());
            // Use addBean directly to avoid lifecycle check - the context may not be started yet
            // during Bootstrap's build phase
            context.addBean(Predefined.BeanProviders.garganttua.toString(), beanRef, this.built);
            log.debug("IExpressionContext successfully registered as bean");
        }

        log.trace("Exiting doPostBuildWithDependency() method");
    }

    /**
     * Synchronizes packages from the InjectionContextBuilder to this builder's
     * packages.
     * This ensures that packages declared in the DI context are also scanned for
     * expression methods.
     */
    private void synchronizePackagesFromContext() {
        log.trace("Entering synchronizePackagesFromContext()");

        support.getUseDependencies().stream()
                .filter(dep -> dep.getDependency().represents(IInjectionContextBuilder.class))
                .findFirst()
                .ifPresent(dep -> dep.synchronizePackagesFromContext(contextPackages -> {
                    int beforeSize = this.packages.size();
                    this.packages.addAll(contextPackages);
                    int addedCount = this.packages.size() - beforeSize;
                    if (addedCount > 0) {
                        log.debug("Synchronized {} new packages from InjectionContextBuilder", addedCount);
                    }
                }));

        log.trace("Exiting synchronizePackagesFromContext()");
    }

    /**
     * Builds a unique signature for a method based on its declaring class, name,
     * and parameter types.
     *
     * @param method the method to build a signature for
     * @return a unique string signature like
     *         "com.example.Beans.bean(java.lang.Class,java.lang.String)"
     */
    private String buildMethodSignature(IMethod method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getDeclaringClass().getName());
        signature.append(".");
        signature.append(method.getName());
        signature.append("(");

        IClass<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            signature.append(paramTypes[i].getName());
        }

        signature.append(")");
        return signature.toString();
    }

    @Override
    public IExpressionContextBuilder provide(IObservableBuilder<?, ?> dependency) {
        if(dependency instanceof IInjectionContextBuilder injectionContext ){
            this.addResolverToInjectionContext(injectionContext);
        }
        return super.provide(dependency);
    }

    private void addResolverToInjectionContext(IInjectionContextBuilder context) {
        IClass<Expression> expressionClass = IClass.getClass(Expression.class);
        IClass<Nullable> nullableClass = IClass.getClass(Nullable.class);
        context.resolvers().withResolver((IClass) expressionClass, (t, e) -> {
            Expression expression = e.getAnnotation(expressionClass);
            if (expression == null)
                return Resolved.notResolved(t, e);
            return new Resolved(true, t, this.built.expression(expression.value()),
                    e.isAnnotationPresent(nullableClass));
        });
    }
}
