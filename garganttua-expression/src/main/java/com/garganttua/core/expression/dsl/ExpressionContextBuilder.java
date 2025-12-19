package com.garganttua.core.expression.dsl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.context.IExpressionNodeFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.dsl.BeanSupplierBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FutureSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Builder for constructing ExpressionContext instances with fluent API.
 *
 * <p>
 * {@code ExpressionBuilder} implements the DSL builder pattern for creating
 * {@link IExpressionContext} objects. It extends
 * {@link AbstractAutomaticBuilder} to
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
public class ExpressionContextBuilder
        extends AbstractAutomaticBuilder<IExpressionContextBuilder, IExpressionContext>
        implements IExpressionContextBuilder {

    private Set<String> packages = new HashSet<>();

    private Set<IExpressionMethodBinderBuilder<?>> nodes = new HashSet<>();

    private IDiContextBuilder injectionContextBuilder;
    private IDiContext injectionContext;

    protected ExpressionContextBuilder() {
        super();
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
    public <T> IExpressionMethodBinderBuilder<T> expression(ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier, Class<T> supplied) {
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
        if (!this.canBuild()) {
            log.atError().log("Attempt to build before authorization, injection context is missing");
            throw new DslException("Build is not yet authorized, injection context is missing");
        }
        Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> builtNodes = this.nodes.stream()
                .map(IExpressionMethodBinderBuilder::build).collect(Collectors.toSet());

        CompletableFuture<ExpressionContext> futur = new CompletableFuture<>();
        try {
            this.expression(new FutureSupplierBuilder<>(futur, ExpressionContext.class), String.class).method(ExpressionContext.class.getMethod("man")).withDescription("the description");
            this.expression(new FutureSupplierBuilder<>(futur, ExpressionContext.class), String.class).method(ExpressionContext.class.getMethod("expressionManualByIndex", int.class)).withDescription("the description");
            this.expression(new FutureSupplierBuilder<>(futur, ExpressionContext.class), String.class).method(ExpressionContext.class.getMethod("expressionManualByKey", String.class)).withDescription("the description");
        } catch (DslException | NoSuchMethodException | SecurityException e) {
            throw new DslException("Failed to register built-in expression nodes", e);
        }

        ExpressionContext context = new ExpressionContext(builtNodes);
        futur.complete(context);
        
        return context;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (!this.canBuild()) {
            log.atError().log("Attempt to build before authorization");
            throw new DslException("Build is not yet authorized");
        }
        List<Method> expressions = new ArrayList<>();
        this.packages.stream().forEach(p -> {
            expressions.addAll(ObjectReflectionHelper.getMethodsWithAnnotation(p, Expression.class));
        });
        expressions.stream().forEach(
                m -> this.expression(new BeanSupplierBuilder<>(m.getDeclaringClass()), m.getReturnType()).method(m).autoDetect(true));
    }

    private boolean canBuild() {
        if (this.injectionContextBuilder == null) {
            log.atWarn().log("Injection context builder is not set, can build");
            return true;
        }
        if (this.injectionContext == null) {
            log.atWarn().log("Injection context is not set, cannot build");
            return false;
        }
        log.atWarn().log("Injection context is set, can build");
        return true;
    }

    @Override
    public IExpressionContextBuilder context(IDiContextBuilder context) {
        this.injectionContextBuilder = Objects.requireNonNull(context, "Injection context builder cannot be null");

        context.observer(this);
        context.resolvers().withResolver(Expression.class, (t, e) -> {
            Expression expression = e.getAnnotation(Expression.class);
            if (expression == null)
                return Resolved.notResolved(t, e);
            return new Resolved(true, t, this.built.expression(expression.value()),
                    e.isAnnotationPresent(Nullable.class));
        });

        return this;
    }

    @Override
    public void handle(IDiContext context) {
        log.atTrace().log("Entering handle() method");
        this.injectionContext = Objects.requireNonNull(context, "Context cannot be null");
        log.atTrace().log("Exiting handle() method");
    }

}
