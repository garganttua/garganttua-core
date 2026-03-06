package com.garganttua.core.expression.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ContextualExpressionNode;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ContextualMethodBinder;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionNodeFactory<R, S extends ISupplier<R>>
        extends ContextualMethodBinder<IExpressionNode<R, S>, IExpressionNodeContext>
        implements IExpressionNodeFactory<R, S> {

    // ========== Fields ==========

    private final IMethod method;
    private final IClass<?>[] parameterTypes;
    private final List<Boolean> nullableParameters;
    private final List<Boolean> lazyParameters;
    @SuppressWarnings("java:S1068")
    private final ObjectAddress methodAddress;
    private final boolean hasGenericReturnType;

    private String name;
    private String description;
    private ISupplier<?> methodOwnerSupplier;

    // ========== Constructors ==========

    public ExpressionNodeFactory(
            ISupplier<?> methodOwnerSupplier,
            Class<S> supplied,
            IMethod method,
            ObjectAddress methodAddress,
            List<Boolean> nullableParameters,
            Optional<String> name, Optional<String> description) throws ExpressionException {

        super(methodOwnerSupplier,
                resolveReflectMethod(methodOwnerSupplier.getSuppliedClass(), method),
                List.of());

        log.atTrace().log("Creating ExpressionNodeFactory: method={}", method.getName());

        this.methodOwnerSupplier = Objects.requireNonNull(methodOwnerSupplier, "Method owner supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.name = name.orElse(this.method.getName());
        this.description = description.orElse("No description");
        this.methodAddress = Objects.requireNonNull(methodAddress, "Method address cannot be null");
        this.parameterTypes = this.method.getParameterTypes();
        this.nullableParameters = Objects.requireNonNull(nullableParameters,
                "Nullable parameters list cannot be null");
        this.lazyParameters = detectLazyParameters();
        this.hasGenericReturnType = method.getTypeParameters().length > 0
                && method.getReturnType().getType() == Object.class;

        validateParameterConfiguration();

        log.atDebug().log("ExpressionNodeFactory created: method={}, parameterCount={}, genericReturn={}",
                method.getName(), parameterTypes.length, hasGenericReturnType);
    }

    // ========== Public Methods ==========

    @Override
    public String key() {
        StringBuilder key = new StringBuilder(this.name);
        key.append("(");

        for (int i = 0; i < this.parameterTypes.length; i++) {
            if (i > 0) {
                key.append(",");
            }
            if (isLazyParameter(i)) {
                key.append("ISupplier");
            } else {
                key.append(this.parameterTypes[i].getSimpleName());
            }
        }

        key.append(")");
        String keyString = key.toString();
        log.atDebug().log("Generated factory key: {}", keyString);
        return keyString;
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> execute(IExpressionNodeContext ownerContext, Object... contexts)
            throws ReflectionException {
        return this.supply(ownerContext, contexts);
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> supply(
            IExpressionNodeContext context,
            Object... otherContexts) throws SupplyException {

        log.atTrace().log("Supplying expression node for contexts {} {}", context, otherContexts);

        if (!context.matches(this.parameterTypes)) {
            log.atDebug().log("Context does not match parameter types, returning empty");
            return Optional.empty();
        }

        IExpressionNode<R, S> expressionNode = context.buildContextual()
                ? createContextualNode(context)
                : createNonContextualNode(context);

        log.atDebug().log("Expression node created: type={}",
                expressionNode != null ? expressionNode.getClass().getSimpleName() : "null");

        IClass<IExpressionNode<R, S>> nodeClass = (IClass<IExpressionNode<R, S>>) (IClass<?>) IClass.getClass(IExpressionNode.class);
        return Optional.ofNullable(expressionNode).map(n -> SingleMethodReturn.of(n, nodeClass));
    }

    // ========== Private Node Creation Methods ==========

    private IExpressionNode<R, S> createNonContextualNode(IExpressionNodeContext context) {
        return (IExpressionNode<R, S>) new ExpressionNode<>(
                getExecutableReference(),
                this::bindNode,
                getReturnType(),
                context.parameters(),
                this.lazyParameters);
    }

    private IExpressionNode<R, S> createContextualNode(IExpressionNodeContext context) {
        return (IExpressionNode<R, S>) new ContextualExpressionNode<>(
                getExecutableReference(),
                (c, params) -> this.bindContextualNode(params),
                getReturnType(),
                context.parameters(),
                this.lazyParameters);
    }

    // ========== Private Binding Methods ==========

    private IContextualSupplier<R, IExpressionContext> bindContextualNode(Object... parameters) {
        log.atTrace().log("Binding contextual node with {} parameters", parameters.length);

        List<ISupplier<?>> encapsulatedParams = encapsulateParameters(parameters);

        ContextualMethodBinder<R, IExpressionContext> binder = new ContextualMethodBinder<>(
                this.methodOwnerSupplier,
                resolveReflectMethod(this.methodOwnerSupplier.getSuppliedClass(), this.method),
                encapsulatedParams);

        return new MethodReturnUnwrappingContextualSupplier<>(binder, getReturnType(), hasGenericReturnType);
    }

    private ISupplier<R> bindNode(Object... parameters) throws DslException {
        log.atTrace().log("Binding non-contextual node with {} parameters", parameters.length);

        List<ISupplier<?>> encapsulatedParams = encapsulateParameters(parameters);

        IMethodBinder<R> binder = new MethodBinder<>(
                this.methodOwnerSupplier,
                resolveReflectMethod(this.methodOwnerSupplier.getSuppliedClass(), this.method),
                encapsulatedParams);

        return new MethodReturnUnwrappingSupplier<>(binder, getReturnType(), hasGenericReturnType);
    }

    // ========== Private Static Helpers ==========

    static ResolvedMethod resolveReflectMethod(IClass<?> ownerType, IMethod method) {
        IReflection reflection = IClass.getReflection();
        IClass<?>[] paramTypes = method.getParameterTypes();
        return MethodResolver.methodByName(ownerType, reflection, method.getName(),
                method.getReturnType(), paramTypes);
    }

    // ========== Private Utility Methods ==========

    private List<ISupplier<?>> encapsulateParameters(Object[] parameters) {
        List<ISupplier<?>> encapsulated = new ArrayList<>(parameters.length);

        for (int i = 0; i < parameters.length; i++) {
            boolean isLazy = isLazyParameter(i);
            ISupplier<?> supplier = null;

            if (isLazy) {
                if (parameters[i] instanceof ISupplier<?> lazySupplier) {
                    supplier = new FixedSupplierBuilder<>(lazySupplier, (IClass) lazySupplier.getSuppliedClass()).build();
                    log.atTrace().log("Encapsulating lazy parameter {} as ISupplier wrapper", i);
                } else {
                    ISupplier<?> literalSupplier = createLiteralSupplier(parameters[i]);
                    supplier = new FixedSupplierBuilder<>(literalSupplier, (IClass) literalSupplier.getSuppliedClass()).build();
                    log.atTrace().log("Encapsulating lazy parameter {} as literal ISupplier wrapper", i);
                }
            } else if (!(parameters[i] instanceof ISupplier<?>)) {
                supplier = new FixedSupplierBuilder<>(parameters[i], (IClass) IClass.getClass(parameters[i].getClass())).build();
            } else {
                supplier = (ISupplier<?>) parameters[i];
            }

            boolean nullable = this.nullableParameters.get(i);
            encapsulated.add(new NullableSupplier<>(supplier, nullable));
        }

        return encapsulated;
    }

    private ISupplier<?> createLiteralSupplier(Object value) {
        return new ISupplier<Object>() {
            @Override
            public Optional<Object> supply() {
                return Optional.ofNullable(value);
            }

            @Override
            public java.lang.reflect.Type getSuppliedType() {
                return value == null ? Object.class : value.getClass();
            }

            @Override
            public IClass<Object> getSuppliedClass() {
                return IClass.getClass(Object.class);
            }
        };
    }

    private IClass<R> getReturnType() {
        return (IClass<R>) this.method.getReturnType();
    }

    private void validateParameterConfiguration() throws ExpressionException {
        if (this.parameterTypes.length != this.nullableParameters.size()) {
            String errorMsg = String.format(
                    "Expression parameters size mismatch: parameterTypes [%d] vs nullableParameters [%d]",
                    this.parameterTypes.length,
                    this.nullableParameters.size());

            log.atError().log(errorMsg);
            throw new ExpressionException(errorMsg);
        }
    }

    private List<Boolean> detectLazyParameters() {
        List<Boolean> lazy = new ArrayList<>(this.parameterTypes.length);
        IClass<?> iSupplierClass = IClass.getClass(ISupplier.class);
        for (IClass<?> paramType : this.parameterTypes) {
            lazy.add(iSupplierClass.isAssignableFrom(paramType));
        }
        log.atTrace().log("Detected lazy parameters for {}: {}", this.method.getName(), lazy);
        return lazy;
    }

    public boolean isLazyParameter(int index) {
        return index >= 0 && index < lazyParameters.size() && Boolean.TRUE.equals(lazyParameters.get(index));
    }

    public List<Boolean> getLazyParameters() {
        return lazyParameters;
    }

    // ========== Inner Classes ==========

    /**
     * Supplier wrapper that unwraps IMethodReturn and dynamically resolves
     * the actual return type from the result value for generic methods.
     */
    private static class MethodReturnUnwrappingSupplier<T> implements ISupplier<T> {
        private final ISupplier<IMethodReturn<T>> delegate;
        private final IClass<T> declaredReturnType;
        private final boolean resolveTypeFromResult;
        private volatile IClass<T> resolvedReturnType;

        MethodReturnUnwrappingSupplier(ISupplier<IMethodReturn<T>> delegate, IClass<T> declaredReturnType, boolean resolveTypeFromResult) {
            this.delegate = delegate;
            this.declaredReturnType = declaredReturnType;
            this.resolveTypeFromResult = resolveTypeFromResult;
        }

        @Override
        public Optional<T> supply() throws SupplyException {
            return delegate.supply()
                    .flatMap(methodReturn -> {
                        if (methodReturn.hasException()) {
                            throw new SupplyException(
                                    "Method invocation failed", methodReturn.getException());
                        }
                        Optional<T> result = methodReturn.firstOptional();
                        if (resolveTypeFromResult && resolvedReturnType == null) {
                            result.ifPresent(value -> {
                                resolvedReturnType = (IClass<T>) IClass.getClass(value.getClass());
                            });
                        }
                        return result;
                    });
        }

        @Override
        public java.lang.reflect.Type getSuppliedType() {
            return getSuppliedClass().getType();
        }

        @Override
        public IClass<T> getSuppliedClass() {
            return resolvedReturnType != null ? resolvedReturnType : declaredReturnType;
        }
    }

    private static class MethodReturnUnwrappingContextualSupplier<T, C> implements IContextualSupplier<T, C> {
        private final IContextualSupplier<IMethodReturn<T>, C> delegate;
        private final IClass<T> declaredReturnType;
        private final boolean resolveTypeFromResult;
        private volatile IClass<T> resolvedReturnType;

        MethodReturnUnwrappingContextualSupplier(IContextualSupplier<IMethodReturn<T>, C> delegate, IClass<T> declaredReturnType, boolean resolveTypeFromResult) {
            this.delegate = delegate;
            this.declaredReturnType = declaredReturnType;
            this.resolveTypeFromResult = resolveTypeFromResult;
        }

        @Override
        public Optional<T> supply(C context, Object... otherContexts) throws SupplyException {
            return delegate.supply(context, otherContexts)
                    .flatMap(methodReturn -> {
                        if (methodReturn.hasException()) {
                            throw new SupplyException(
                                    "Method invocation failed", methodReturn.getException());
                        }
                        Optional<T> result = methodReturn.firstOptional();
                        if (resolveTypeFromResult && resolvedReturnType == null) {
                            result.ifPresent(value -> {
                                resolvedReturnType = (IClass<T>) IClass.getClass(value.getClass());
                            });
                        }
                        return result;
                    });
        }

        @Override
        public IClass<C> getOwnerContextType() {
            return delegate.getOwnerContextType();
        }

        @Override
        public java.lang.reflect.Type getSuppliedType() {
            return getSuppliedClass().getType();
        }

        @Override
        public IClass<T> getSuppliedClass() {
            return resolvedReturnType != null ? resolvedReturnType : declaredReturnType;
        }
    }

    class ExpressionMethodBinderBuilder
            extends AbstractMethodBinderBuilder<R, ExpressionMethodBinderBuilder, Object, IMethodBinder<R>> {

        protected ExpressionMethodBinderBuilder(Object up, ISupplierBuilder<?, ?> supplier) throws DslException {
            super(up, supplier, java.util.Set.of());
        }

        @Override
        protected void doAutoDetection() throws DslException {
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

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public String man() {
        StringBuilder manual = new StringBuilder();

        manual.append("NAME\n");
        manual.append("    ").append(this.name).append(" - ").append("\n\n");

        manual.append("SYNOPSIS\n");
        manual.append("    ").append(this.method.getReturnType().getSimpleName()).append(" ");
        manual.append(this.name).append("(");

        IParameter[] params = this.method.getParameters();
        for (int i = 0; i < this.parameterTypes.length; i++) {
            if (i > 0) {
                manual.append(", ");
            }
            manual.append(this.parameterTypes[i].getSimpleName());
            manual.append(" ").append(params[i].getName());
        }
        manual.append(")\n\n");

        manual.append("DESCRIPTION\n");
        manual.append("    ").append(this.description).append("\n\n");

        if (this.parameterTypes.length > 0) {
            manual.append("PARAMETERS\n");
            for (int i = 0; i < this.parameterTypes.length; i++) {
                manual.append("    ").append(params[i].getName()).append(" : ");
                manual.append(this.parameterTypes[i].getSimpleName());

                if (this.nullableParameters.get(i).booleanValue()) {
                    manual.append(" (nullable)");
                } else {
                    manual.append(" (required)");
                }
                manual.append("\n");
            }
            manual.append("\n");
        }

        manual.append("RETURN VALUE\n");
        manual.append("    ").append(this.method.getReturnType().getSimpleName()).append("\n");

        return manual.toString();
    }
}
