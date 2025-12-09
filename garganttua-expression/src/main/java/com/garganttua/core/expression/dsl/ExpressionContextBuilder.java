package com.garganttua.core.expression.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.IExpressionContext;

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
 *     .create()
 *     .withExpression(Calculator.class, Integer.class)
 *         .method("add")
 *         .withParam(5)
 *         .withParam(3)
 *         .end()
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class ExpressionContextBuilder
        extends AbstractAutomaticBuilder<IExpressionContextBuilder, IExpressionContext>
        implements IExpressionContextBuilder {

    private List<String> packages = new ArrayList<>();

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
    public static ExpressionContextBuilder create() {
        log.atTrace().log("Creating new ExpressionBuilder");
        return new ExpressionContextBuilder();
    }

    @Override
    public <T> IExpressionMethodBinderBuilder<T> withExpression(Class<?> methodOwner, Class<T> supplied) {
        log.atDebug().log("Creating ExpressionMethodBinderBuilder for methodOwner={}, supplied={}",
                methodOwner, supplied);
        Objects.requireNonNull(methodOwner, "Method owner cannot be null");
        Objects.requireNonNull(supplied, "Supplied type cannot be null");
        return new ExpressionMethodBinderBuilder<>(this, methodOwner, supplied);
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
        throw new UnsupportedOperationException("Unimplemented method 'doBuild'");
    }

    @Override
    protected void doAutoDetection() throws DslException {
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }
}
