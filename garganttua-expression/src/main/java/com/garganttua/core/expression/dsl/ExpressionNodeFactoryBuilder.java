package com.garganttua.core.expression.dsl;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.expression.context.ExpressionNodeFactory;
import com.garganttua.core.expression.context.IExpressionNodeFactory;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionNodeFactoryBuilder<S>
        extends
        AbstractMethodBinderBuilder<IExpressionNode<S, ISupplier<S>>, IExpressionMethodBinderBuilder<S>, IExpressionContextBuilder, IExpressionNodeFactory<S, ISupplier<S>>>
        implements IExpressionMethodBinderBuilder<S> {

    private ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier;
    @SuppressWarnings("unused")
    private IClass<S> supplied;
    private String name;
    private String description = "No description";
    private IObjectQuery<?> objectQuery;

    public ExpressionNodeFactoryBuilder(IExpressionContextBuilder parent,
            ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier,
            IClass<S> supplied) throws DslException {
        super(parent, methodOwnerSupplier, java.util.Set.of());
        log.atTrace().log(
                "Entering ExpressionMethodBinderBuilder constructor with methodOwnerSupplier={}, supplied={}",
                methodOwnerSupplier, supplied);
        this.methodOwnerSupplier = Objects.requireNonNull(methodOwnerSupplier, "Method owner supplier cannot be null");
        this.supplied = Objects.requireNonNull(supplied, "Supplied type cannot be null");
        try {
            this.objectQuery = IClass.getReflection().query(this.methodOwnerSupplier.getSuppliedClass());
        } catch (ReflectionException e) {
            log.atError().log("[MethodBinderBuilder] Error creating objectQuery for class {}",
                    this.methodOwnerSupplier.getSuppliedClass(), e);
            throw new DslException(e.getMessage(), e);
        }
        log.atTrace().log("Exiting ExpressionMethodBinderBuilder constructor");
    }

    @Override
    protected IExpressionNodeFactory<S, ISupplier<S>> doBuild() throws DslException {
        IMethod method = method();
        ObjectAddress methodAddress = this.methodAddress();

        return new ExpressionNodeFactory<>(
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
        IMethod m = method();
        IClass<Expression> expressionClass = IClass.getClass(Expression.class);
        Expression nodeInfos = m.getAnnotation(expressionClass);
        if (nodeInfos.name() != null && !nodeInfos.name().isBlank()) {
            this.withName(nodeInfos.name());
        } else {
            this.name = m.getName();
        }
        if (nodeInfos.description() != null && !nodeInfos.description().isBlank()) {
            this.withDescription(nodeInfos.description());
        }
        IClass<Nullable> nullableClass = IClass.getClass(Nullable.class);
        IParameter[] params = m.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(nullableClass)) {
                this.withNullableParam(i);
            }
        }
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withNullableParam(int i) {
        this.nullableParameter(i, true);
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> encapsulatedMethod(ObjectAddress methodAddress, IClass<S> returnType,
            IClass<?>... parameterTypes) throws DslException {
        IMethod methodObject = (IMethod) MethodResolver
                .selectBestMatch(this.objectQuery.findAll(methodAddress), returnType, parameterTypes,
                        this.methodOwnerSupplier.getSuppliedClass())
                .getLast();

        this.method(methodObject);

        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> encapsulatedMethod(String methodName, IClass<S> returnType,
            IClass<?>... parameterTypes) throws DslException {
        ObjectAddress methodAddress = this.objectQuery.address(methodName);
        return this.encapsulatedMethod(methodAddress, returnType, parameterTypes);
    }

    @Override
    @Deprecated
    public IExpressionMethodBinderBuilder<S> method(String methodName,
            IClass<IExpressionNode<S, ISupplier<S>>> returnType,
            IClass<?>... parameterTypes) throws DslException {
        log.atWarn().log("method is not supported for ExpressionMethodBinderBuilder");
        return this;
    }

    @Override
    @Deprecated
    public IExpressionMethodBinderBuilder<S> method(ObjectAddress methodAddress,
            IClass<IExpressionNode<S, ISupplier<S>>> returnType, IClass<?>... parameterTypes) throws DslException {
        log.atWarn().log("method is not supported for ExpressionMethodBinderBuilder");
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
    public IExpressionMethodBinderBuilder<S> withName(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        return this;
    }

    @Override
    public IExpressionMethodBinderBuilder<S> withDescription(String description) {
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        return this;
    }

    @Override
    protected void doPreBuildWithDependency_(Object dependency) {
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
    }
}
