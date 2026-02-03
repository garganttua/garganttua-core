package com.garganttua.core.expression.dsl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
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
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FutureSupplierBuilder;
import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

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

    private final Set<String> packages = new HashSet<>();
    private final Set<IExpressionMethodBinderBuilder<?>> nodes = new HashSet<>();

    protected ExpressionContextBuilder() {
        super(Set.of(IInjectionContextBuilder.class), Set.of());
        log.atTrace().log("Entering ExpressionBuilder constructor");
        log.atTrace().log("Exiting ExpressionBuilder constructor");
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
    public <T> IExpressionMethodBinderBuilder<T> expression(
            ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier, Class<T> supplied) {
        log.atDebug().log("Creating ExpressionMethodBinderBuilder for methodOwnerSupplier={}, supplied={}",
                methodOwnerSupplier, supplied);
        Objects.requireNonNull(methodOwnerSupplier, "Method owner supplier cannot be null");
        Objects.requireNonNull(supplied, "Supplied type cannot be null");
        IExpressionMethodBinderBuilder<T> expressionNodeMethodBinderBuilder = new ExpressionNodeFactoryBuilder<>(this,
                methodOwnerSupplier, supplied);
        this.nodes.add(expressionNodeMethodBinderBuilder);
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
            this.expression(new FutureSupplierBuilder<>(futur, ExpressionContext.class), String.class)
                    .method(ExpressionContext.class.getMethod("man")).withDescription("the description");
            this.expression(new FutureSupplierBuilder<>(futur, ExpressionContext.class), String.class)
                    .method(ExpressionContext.class.getMethod("man", int.class)).withDescription("the description");
            this.expression(new FutureSupplierBuilder<>(futur, ExpressionContext.class), String.class)
                    .method(ExpressionContext.class.getMethod("man", String.class)).withDescription("the description");
        } catch (DslException | NoSuchMethodException | SecurityException e) {
            throw new DslException("Failed to register built-in expression nodes", e);
        }
        Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> builtNodes = this.nodes.stream()
                .map(IExpressionMethodBinderBuilder::build).collect(Collectors.toSet());

        ExpressionContext context = new ExpressionContext(builtNodes);
        futur.complete(context);

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

        List<Method> expressions = new ArrayList<>();
        this.packages
                .forEach(p -> expressions.addAll(ObjectReflectionHelper.getMethodsWithAnnotation(p, Expression.class)));

        // Deduplicate methods by signature (declaring class + method name + parameter
        // types)
        // because distinct() only works with object identity, not method equivalence
        Map<String, Method> uniqueMethods = new LinkedHashMap<>();
        int duplicateCount = 0;
        for (Method m : expressions) {
            String signature = buildMethodSignature(m);
            if (uniqueMethods.putIfAbsent(signature, m) != null) {
                duplicateCount++;
            }
        }

        log.atDebug().log(
                "Found {} total methods with @Expression, {} unique after deduplication ({} duplicates removed)",
                expressions.size(), uniqueMethods.size(), duplicateCount);

        // Create factories for unique methods only
        uniqueMethods.values()
                .forEach(m -> this.expression(new BeanSupplierBuilder<>(m.getDeclaringClass()), m.getReturnType())
                        .method(m).autoDetect(true));
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
                    IExpressionContext.class,
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
                .filter(dep -> dep.getDependency().equals(IInjectionContextBuilder.class))
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
    private String buildMethodSignature(Method method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getDeclaringClass().getName());
        signature.append(".");
        signature.append(method.getName());
        signature.append("(");

        Class<?>[] paramTypes = method.getParameterTypes();
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
        context.resolvers().withResolver(Expression.class, (t, e) -> {
            Expression expression = e.getAnnotation(Expression.class);
            if (expression == null)
                return Resolved.notResolved(t, e);
            return new Resolved(true, t, this.built.expression(expression.value()),
                    e.isAnnotationPresent(Nullable.class));
        });
    }
}
