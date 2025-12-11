package com.garganttua.core.expression.context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ContextualExpressionNode;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionLeaf;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionNodeFactory<R, S extends ISupplier<R>> extends
        ContextualMethodBinder<IExpressionNode<R, S>, IExpressionNodeContext> implements IExpressionNodeFactory<R, S> {

    private Boolean leaf;
    private Method method;
    private Class<?>[] parameterTypes;
    private List<Boolean> nullableParameters;

    class ExpressionMethodBinderBuilder
            extends AbstractMethodBinderBuilder<R, ExpressionMethodBinderBuilder, Object, IMethodBinder<R>> {

        protected ExpressionMethodBinderBuilder(Object up, ISupplierBuilder<?, ?> supplier) throws DslException {
            super(up, supplier);
        }

        @Override
        protected void doAutoDetection() throws DslException {
        }
    }

    // unused ctor
    private ExpressionNodeFactory(ISupplier<?> objectSupplier, ObjectAddress method,
            List<ISupplier<?>> parameterSuppliers, Class<IExpressionNode<R, S>> returnedClass)
            throws ExpressionException {
        super(objectSupplier, method, parameterSuppliers, returnedClass);
    }

    @SuppressWarnings("unchecked")
    public ExpressionNodeFactory(Class<?> methodOwner, Class<S> supplied, Method method, ObjectAddress methodAddress,
            List<Boolean> nullableParameters, Boolean leaf) throws ExpressionException {
        super(new NullSupplier<>(methodOwner), methodAddress, List.of(),
                (Class<IExpressionNode<R, S>>) (Class<?>) IExpressionNode.class);
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.parameterTypes = this.method.getParameterTypes();
        this.nullableParameters = Objects.requireNonNull(nullableParameters, "Nullable parameters list cannot be null");
        if (this.parameterTypes.length != this.nullableParameters.size()) {
            throw new ExpressionException("Expression parameters size mismatch : parameterTypes ["
                    + this.parameterTypes.length + "] vs nullableParameters [" + this.nullableParameters.size() + "]");
        }

        this.leaf = leaf;
    }

    @Override
    public String getKey() {
        return "";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<IExpressionNode<R, S>> supply(IExpressionNodeContext context, Object... otherContexts)
            throws SupplyException {

        IExpressionNode<R, S> expressionNode = null;
        if (context.matches(this.parameterTypes)) {
            if (!this.leaf) {
                if (context.buildContextual()) {
                    expressionNode = (IExpressionNode<R, S>) new ExpressionNode<R>(
                            getExecutableReference(),
                            params -> {
                                return this.bindNode(params);
                            },
                            (Class<R>) this.method.getReturnType(),
                            context.nodeChilds());
                } else {
                    expressionNode = (IExpressionNode<R, S>) new ContextualExpressionNode<R>(
                            getExecutableReference(),
                            (c, params) -> {
                                return this.bindContextualNode(params);
                            },
                            (Class<R>) this.method.getReturnType(),
                            context.nodeChilds());
                }
            } else {
                expressionNode = (IExpressionNode<R, S>) new ExpressionLeaf<R>(
                        getExecutableReference(),
                        params -> {
                            return this.bindLeaf(params);
                        },
                        (Class<R>) this.method.getReturnType(),
                        context.leafParameters());
            }
        }
        return Optional.ofNullable(expressionNode);
    }

    @SuppressWarnings("unchecked")
    private IContextualSupplier<R, IExpressionContext> bindContextualNode(ISupplier<?>[] parameters) {
        List<ISupplier<?>> nullableEncapsulated = this.encapsulateParameters(parameters);
        return new ContextualMethodBinder<>(
                new NullSupplier<>(this.method.getDeclaringClass()),
                new ObjectAddress(this.method.getName()),
                nullableEncapsulated,
                (Class<R>) this.method.getReturnType());

    }

    @SuppressWarnings("unchecked")
    private ISupplier<R> bindNode(ISupplier<?>[] parameters) throws DslException {
        List<ISupplier<?>> nullableEncapsulated = this.encapsulateParameters(parameters);

        return new MethodBinder<R>(
                new NullSupplier<>(this.method.getDeclaringClass()),
                new ObjectAddress(this.method.getName()),
                nullableEncapsulated,
                (Class<R>) this.method.getReturnType());
    }

    private List<ISupplier<?>> encapsulateParameters(ISupplier<?>[] parameters) {
        List<ISupplier<?>> nullableEncapsulated = new ArrayList<>();

        for (int i = 0; i < parameters.length; i++) {
            nullableEncapsulated.add(new NullableSupplier<>(parameters[i], this.nullableParameters.get(i)));
        }
        return nullableEncapsulated;
    }

    @SuppressWarnings("unchecked")
    private IMethodBinder<R> bindLeaf(Object[] parameters) {
        ExpressionMethodBinderBuilder b = new ExpressionMethodBinderBuilder(new Object(),
                new NullSupplierBuilder<>(this.method.getDeclaringClass())).method(this.method)
                .withReturn((Class<R>) this.method.getReturnType());

        for (int i = 0; i < parameters.length; i++) {
            b.withParam(i, new FixedSupplierBuilder<>(parameters[i]),
                    this.nullableParameters.get(i));
        }
        return b.build();
    }

}
