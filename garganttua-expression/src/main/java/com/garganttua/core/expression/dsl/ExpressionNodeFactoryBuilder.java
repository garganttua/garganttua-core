package com.garganttua.core.expression.dsl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.expression.context.ExpressionNodeFactory;
import com.garganttua.core.expression.context.IExpressionNodeFactory;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Builder for constructing ExpressionNodeFactory instances with fluent API.
 *
 * <p>
 * {@code ExpressionMethodBinderBuilder} extends
 * {@link AbstractMethodBinderBuilder}
 * to provide specialized building of method binders for expression contexts.
 * It allows binding methods to expressions with parameters and return to the
 * parent expression builder.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * ExpressionBuilder builder = ExpressionBuilder.create();
 *
 * builder.withExpression(StringUtils.class, String.class)
 *         .method("concat")
 *         .withParam("Hello")
 *         .withParam(" World")
 *         .end();
 * }</pre>
 *
 * @param <S> the supplied type
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class ExpressionNodeFactoryBuilder<S>
        extends
        AbstractMethodBinderBuilder<IExpressionNode<S, ISupplier<S>>, IExpressionMethodBinderBuilder<S>, IExpressionContextBuilder, IExpressionNodeFactory<S, ISupplier<S>>>
        implements IExpressionMethodBinderBuilder<S> {

    private ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier;
    @SuppressWarnings("unused")
    private Class<S> supplied;
    private String name;
    private String description = "No description";

    public ExpressionNodeFactoryBuilder(IExpressionContextBuilder parent,
            ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier,
            Class<S> supplied) throws DslException {
        super(parent, methodOwnerSupplier);
        log.atTrace().log(
                "Entering ExpressionMethodBinderBuilder constructor with methodOwnerSupplier={}, supplied={}",
                methodOwnerSupplier, supplied);
        this.methodOwnerSupplier = Objects.requireNonNull(methodOwnerSupplier, "Method owner supplier cannot be null");
        this.supplied = Objects.requireNonNull(supplied, "Supplied type cannot be null");
        log.atTrace().log("Exiting ExpressionMethodBinderBuilder constructor");
    }

    // ========== Override method() methods to verify static ==========

    @Override
    public IExpressionMethodBinderBuilder<S> method(Method method) throws DslException {
        log.atDebug().log("Checking if method {} is static", method.getName());
        if (!Methods.isStatic(method)) {
            throw new DslException("Method " + method.getName() + " must be static for expression binding");
        }
        this.name = method.getName();
        return super.method(method);
    }

    @Override
    public IExpressionMethodBinderBuilder<S> method(ObjectAddress methodAddress) throws DslException {
        log.atDebug().log("Checking if method at address {} is static", methodAddress);
        if (!Methods.isStatic(this.methodOwnerSupplier.getSuppliedClass(), methodAddress)) {
            throw new DslException("Method at address " + methodAddress + " must be static for expression binding");
        }
        this.name = methodAddress.getElement(methodAddress.length()-1);
        return super.method(methodAddress);
    }

    @Override
    public IExpressionMethodBinderBuilder<S> method(String methodName) throws DslException {
        // First call super to resolve the method
        IExpressionMethodBinderBuilder<S> result = super.method(methodName);

        // Then verify it's static
        Method resolvedMethod = super.method();
        log.atDebug().log("Checking if method {} is static", methodName);
        if (!Methods.isStatic(resolvedMethod)) {
            throw new DslException("Method " + methodName + " must be static for expression binding");
        }
        this.name = methodName;
        return result;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> method(Method method, Class<IExpressionNode<S, ISupplier<S>>> returnType,
            Class<?>... parameterTypes) throws DslException {
        log.atDebug().log("Checking if method {} is static", method.getName());
        if (!Methods.isStatic(method)) {
            throw new DslException("Method " + method.getName() + " must be static for expression binding");
        }
        this.name = method.getName();
        return super.method(method, returnType, parameterTypes);
    }

    @Override
    public IExpressionMethodBinderBuilder<S> method(ObjectAddress methodAddress,
            Class<IExpressionNode<S, ISupplier<S>>> returnType, Class<?>... parameterTypes) throws DslException {
        log.atDebug().log("Checking if method at address {} is static", methodAddress);
        if (!Methods.isStatic(this.methodOwnerSupplier.getSuppliedClass(), methodAddress)) {
            throw new DslException("Method at address " + methodAddress + " must be static for expression binding");
        }
        return super.method(methodAddress, returnType, parameterTypes);
    }

    @Override
    public IExpressionMethodBinderBuilder<S> method(String methodName,
            Class<IExpressionNode<S, ISupplier<S>>> returnType, Class<?>... parameterTypes) throws DslException {
        // First call super to resolve the method
        IExpressionMethodBinderBuilder<S> result = super.method(methodName, returnType, parameterTypes);

        // Then verify it's static
        Method resolvedMethod = super.method();
        log.atDebug().log("Checking if method {} is static", methodName);
        if (!Methods.isStatic(resolvedMethod)) {
            throw new DslException("Method " + methodName + " must be static for expression binding");
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected IExpressionNodeFactory<S, ISupplier<S>> doBuild() throws DslException {
        Method method = this.findMethod();
        ObjectAddress methodAddress = new ObjectAddress(method.getName());
        return new ExpressionNodeFactory<S, ISupplier<S>>(
                this.methodOwnerSupplier.build(),
                (Class<ISupplier<S>>) (Class<?>) ISupplier.class,
                method,
                methodAddress,
                this.nullableParameters(),
                Optional.ofNullable(this.name),
                Optional.ofNullable(this.description));
    }

    @Override
    protected void doAutoDetection() throws DslException {
        Method m = this.findMethod();
        Expression nodeInfos = m.getAnnotation(Expression.class);
        if (nodeInfos.name() != null && !nodeInfos.name().isBlank()) {
            this.withName(nodeInfos.name());
        } else {
            this.name = m.getName();
        }
        if (nodeInfos.description() != null && !nodeInfos.description().isBlank()) {
            this.withDescription(nodeInfos.description());
        }
        Parameter[] params = m.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(Nullable.class)) {
                this.withNullableParam(i);
            }
        }
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withNullableParam(int i) {
        this.nullableParameter(i, true);
        return this;
    }

    // ========== Override withParam methods to make them inoperative ==========

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(int i, Object parameter) throws DslException {
        log.atWarn().log("withParam(int, Object) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(int i, Object parameter, boolean nullAllowed)
            throws DslException {
        log.atWarn().log("withParam(int, Object, boolean) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(int i, ISupplierBuilder<?, ?> supplierBuilder)
            throws DslException {
        log.atWarn().log("withParam(int, ISupplierBuilder) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(int i, ISupplierBuilder<?, ?> supplierBuilder,
            boolean nullAllowed) throws DslException {
        log.atWarn()
                .log("withParam(int, ISupplierBuilder, boolean) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(String parameterName, Object parameter) throws DslException {
        log.atWarn().log("withParam(String, Object) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(String parameterName, Object parameter, boolean nullAllowed)
            throws DslException {
        log.atWarn().log("withParam(String, Object, boolean) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(String parameterName, ISupplierBuilder<?, ?> supplierBuilder)
            throws DslException {
        log.atWarn().log("withParam(String, ISupplierBuilder) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(String parameterName, ISupplierBuilder<?, ?> supplierBuilder,
            boolean nullAllowed) throws DslException {
        log.atWarn()
                .log("withParam(String, ISupplierBuilder, boolean) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(Object parameter) throws DslException {
        log.atWarn().log("withParam(Object) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(Object parameter, boolean nullAllowed) throws DslException {
        log.atWarn().log("withParam(Object, boolean) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(ISupplierBuilder<?, ?> supplierBuilder) throws DslException {
        log.atWarn().log("withParam(ISupplierBuilder) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withParam(ISupplierBuilder<?, ?> supplierBuilder, boolean nullAllowed)
            throws DslException {
        log.atWarn().log("withParam(ISupplierBuilder, boolean) is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    // ========== Override withReturn to make it inoperative ==========

    @Override
    public IExpressionMethodBinderBuilder<S> withReturn(Class<IExpressionNode<S, ISupplier<S>>> returnedType)
            throws DslException {
        log.atWarn().log("withReturn is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withName(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withDescription(String description) {
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        return this;
    }
}
