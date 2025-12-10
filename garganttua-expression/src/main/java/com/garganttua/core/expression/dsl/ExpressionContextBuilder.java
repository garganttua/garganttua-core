package com.garganttua.core.expression.dsl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.IExpressionContext;
import com.garganttua.core.expression.annotations.ExpressionLeaf;
import com.garganttua.core.expression.annotations.ExpressionNode;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for constructing ExpressionContext instances with fluent API.
 *
 * <p>
 * {@code ExpressionBuilder} implements the DSL builder pattern for creating
 * {@link IExpressionContext} objects. It extends {@link AbstractAutomaticBuilder} to
 * provide automatic configuration detection and package scanning capabilities.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IExpressionContext context = ExpressionBuilder
 *     .builder()
 *     .withExpressionNode(Calculator.class, Integer.class)
 *         .method("add")
 *         .up()
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class ExpressionContextBuilder
        extends AbstractAutomaticBuilder<IExpressionContextBuilder, IExpressionContext>
        implements IExpressionContextBuilder {

    private Set<String> packages = new HashSet<>();

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
    public <T> IExpressionMethodBinderBuilder<T> withExpressionNode(Class<?> methodOwner, Class<T> supplied) {
        log.atDebug().log("Creating ExpressionMethodBinderBuilder for methodOwner={}, supplied={}",
                methodOwner, supplied);
        Objects.requireNonNull(methodOwner, "Method owner cannot be null");
        Objects.requireNonNull(supplied, "Supplied type cannot be null");
        return new ExpressionNodeMethodBinderBuilder<>(this, methodOwner, supplied);
    }

    @Override
    public <T> IExpressionMethodBinderBuilder<T> withExpressionLeaf(Class<?> methodOwner, Class<T> supplied) {
        log.atDebug().log("Creating ExpressionMethodBinderBuilder for methodOwner={}, supplied={}",
                methodOwner, supplied);
        Objects.requireNonNull(methodOwner, "Method owner cannot be null");
        Objects.requireNonNull(supplied, "Supplied type cannot be null");
        return new ExpressionNodeMethodBinderBuilder<>(this, methodOwner, supplied, true);
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
        
        return null;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        List<Method> nodes = new ArrayList<>();
        List<Method> leafs = new ArrayList<>();
        this.packages.stream().forEach(p -> {
            nodes.addAll(ObjectReflectionHelper.getMethodsWithAnnotation(p, ExpressionNode.class));
            leafs.addAll(ObjectReflectionHelper.getMethodsWithAnnotation(p, ExpressionLeaf.class));
        });
        nodes.stream().forEach(m -> this.withExpressionNode(m.getDeclaringClass(), m.getReturnType()).method(m).autoDetect(true));
        leafs.stream().forEach(m -> this.withExpressionLeaf(m.getDeclaringClass(), m.getReturnType()).method(m).autoDetect(true));
    }
}
