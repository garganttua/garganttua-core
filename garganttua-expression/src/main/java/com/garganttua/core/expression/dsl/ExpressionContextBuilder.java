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

import java.lang.reflect.Modifier;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@Bootstrap
public class ExpressionContextBuilder
        extends AbstractAutomaticDependentBuilder<IExpressionContextBuilder, IExpressionContext>
        implements IExpressionContextBuilder {

    private static final String SOURCE_EXPLICIT = "explicit";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";

    private final Set<String> packages = new HashSet<>();
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
        log.atTrace().log("Entering ExpressionBuilder constructor");

        this.nodeCollector = new MultiSourceCollector<>();
        nodeCollector.source(nodeSetSupplier(explicitNodes), 0, SOURCE_EXPLICIT);
        nodeCollector.source(nodeSetSupplier(autoDetectedNodes), 1, SOURCE_AUTO_DETECTED);

        log.atTrace().log("Exiting ExpressionBuilder constructor");
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
        log.atTrace().log("Creating new ExpressionBuilder");
        return new ExpressionContextBuilder();
    }

    @Override
    public IExpressionContextBuilder observer(IBuilderObserver<IExpressionContextBuilder, IExpressionContext> observer) {
        log.atTrace().log("Entering observer(observer={})", observer);
        Objects.requireNonNull(observer, "Observer cannot be null");

        this.observers.add(observer);
        log.atDebug().log("Added observer: {}", observer);

        // If context is already built, notify the observer immediately
        if (this.built != null) {
            observer.handle(this.built);
            log.atDebug().log("Context already built, immediately notified observer: {}", observer);
        }

        log.atTrace().log("Exiting observer");
        return this;
    }

    private void notifyObservers(IExpressionContext built) {
        log.atTrace().log("Entering notifyObserver(built={})", built);
        this.observers.parallelStream().forEach(observer -> {
            observer.handle(built);
            log.atDebug().log("Notified observer: {}", observer);
        });
        log.atTrace().log("Exiting notifyObserver");
    }

    @Override
    public <T> IExpressionMethodBinderBuilder<T> expression(
            ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier, IClass<T> supplied) {
        log.atDebug().log("Creating ExpressionMethodBinderBuilder for methodOwnerSupplier={}, supplied={}",
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
        log.atDebug().log("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IExpressionContextBuilder withPackages(String[] packageNames) {
        log.atDebug().log("Adding {} packages", packageNames.length);
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

        log.atDebug().log(
                "Found {} total methods with @Expression, {} unique after deduplication ({} duplicates removed)",
                expressions.size(), uniqueMethods.size(), duplicateCount);

        if (uniqueMethods.isEmpty() && !this.packages.isEmpty()) {
            log.atWarn().log(
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
        log.atTrace().log("Entering doAutoDetectionWithDependency() with dependency: {}", dependency);
        // No dependency-based auto-detection needed
        log.atTrace().log("Exiting doAutoDetectionWithDependency() method");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPreBuildWithDependency() with dependency: {}", dependency);
        // Nothing to do in pre-build phase
        log.atTrace().log("Exiting doPreBuildWithDependency() method");
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPostBuildWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            log.atDebug().log("Registering IExpressionContext as bean in InjectionContext");
            BeanReference<IExpressionContext> beanRef = new BeanReference<>(
                    IClass.getClass(IExpressionContext.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.empty(),
                    Set.of());
            // Use addBean directly to avoid lifecycle check - the context may not be started yet
            // during Bootstrap's build phase
            context.addBean(Predefined.BeanProviders.garganttua.toString(), beanRef, this.built);
            log.atDebug().log("IExpressionContext successfully registered as bean");
        }

        log.atTrace().log("Exiting doPostBuildWithDependency() method");
    }

    /**
     * Synchronizes packages from the InjectionContextBuilder to this builder's
     * packages.
     * This ensures that packages declared in the DI context are also scanned for
     * expression methods.
     */
    private void synchronizePackagesFromContext() {
        log.atTrace().log("Entering synchronizePackagesFromContext()");

        support.getUseDependencies().stream()
                .filter(dep -> dep.getDependency().represents(IInjectionContextBuilder.class))
                .findFirst()
                .ifPresent(dep -> dep.synchronizePackagesFromContext(contextPackages -> {
                    int beforeSize = this.packages.size();
                    this.packages.addAll(contextPackages);
                    int addedCount = this.packages.size() - beforeSize;
                    if (addedCount > 0) {
                        log.atDebug().log("Synchronized {} new packages from InjectionContextBuilder", addedCount);
                    }
                }));

        log.atTrace().log("Exiting synchronizePackagesFromContext()");
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
