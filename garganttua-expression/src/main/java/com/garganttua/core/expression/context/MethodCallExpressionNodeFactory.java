package com.garganttua.core.expression.context;

import java.lang.reflect.Type;
import java.util.ArrayList;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.MethodBinder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.supply.SupplyException;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodCallExpressionNodeFactory<R, S extends ISupplier<R>> implements IExpressionNodeFactory<R, S> {

    private ResolvedMethod resolved;
    private ExpressionNodeFactory<R, S> factory;
    private List<Boolean> nullables;

    // Deferred resolution fields (when owner type is Object at compile time)
    private boolean deferred;
    private String deferredMethodName;
    private IClass<?>[] deferredParameterTypes;
    private ISupplier<?> deferredOwnerSupplier;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MethodCallExpressionNodeFactory(IExpressionNode<?, S> ownerNode, String methodName, IClass<?>[] parameterTypes)
            throws ExpressionException {
        Objects.requireNonNull(ownerNode, "Owner node cannot be null");
        Objects.requireNonNull(methodName, "Method name cannot be null");
        Objects.requireNonNull(parameterTypes, "Parameter types array cannot be null");

        IClass<?> ownerIClass = ownerNode.getFinalSuppliedClass();
        ISupplier<?> ownerSupplier;

        if (ownerIClass.getType() == Class.class || IClass.getClass(IClass.class).isAssignableFrom(ownerIClass)) {
            // Static method call - evaluate the owner node to get the actual target class
            Object classResult = ownerNode.evaluate().supply()
                    .orElseThrow(() -> new ExpressionException("Cannot resolve class for static method call: " + methodName));
            if (classResult instanceof IClass<?> ic) {
                ownerIClass = ic;
            } else if (classResult instanceof Class<?> c) {
                ownerIClass = IClass.getClass(c);
            } else {
                throw new ExpressionException("Cannot resolve class for static method call: " + methodName);
            }
            ownerSupplier = new NullSupplier<>(ownerIClass);
        } else {
            // Instance method call
            ownerSupplier = ownerNode.evaluate();
        }

        // When the owner type is Object (e.g. from a generic method like cast()),
        // defer method resolution to runtime when the actual type is known
        if (ownerIClass.getType() == Object.class) {
            log.atDebug().log("Deferring method resolution for {}() - owner type is Object", methodName);
            this.deferred = true;
            this.deferredMethodName = methodName;
            this.deferredParameterTypes = parameterTypes;
            this.deferredOwnerSupplier = ownerSupplier;
            return;
        }

        IReflection reflection = IClass.getReflection();
        this.resolved = MethodResolver.methodByName(ownerIClass, reflection, methodName, null, parameterTypes);
        this.nullables = nullableMask(this.resolved);

        this.factory = new ExpressionNodeFactory(ownerSupplier, (Class) this.resolved.getReturnType().getType(),
                this.resolved, this.resolved.address(), nullables, Optional.of(methodName),
                Optional.of("No description available"));
    }

    private List<Boolean> nullableMask(IMethod method) {
        Objects.requireNonNull(method, "method cannot be null");
        IClass<Nullable> nullableClass = IClass.getClass(Nullable.class);
        IParameter[] parameters = method.getParameters();
        List<Boolean> result = new ArrayList<>(parameters.length);
        for (IParameter parameter : parameters) {
            result.add(parameter.isAnnotationPresent(nullableClass));
        }

        return result;
    }

    @Override
    public String getExecutableReference() {
        if (this.deferred) {
            return ":" + this.deferredMethodName;
        }
        return this.factory.getExecutableReference();
    }

    @Override
    public Set<IClass<?>> dependencies() {
        if (this.deferred) {
            return Set.of();
        }
        return this.factory.dependencies();
    }

    @Override
    public Type getSuppliedType() {
        if (this.deferred) {
            return Object.class;
        }
        return this.factory.getSuppliedType();
    }

    @Override
    public IClass<IMethodReturn<IExpressionNode<R, S>>> getSuppliedClass() {
        if (this.deferred) {
            return (IClass<IMethodReturn<IExpressionNode<R, S>>>) (IClass<?>) IClass.getClass(IMethodReturn.class);
        }
        return this.factory.getSuppliedClass();
    }

    @Override
    public IClass<IExpressionNodeContext> getOwnerContextType() {
        if (this.deferred) {
            return IClass.getClass(IExpressionNodeContext.class);
        }
        return this.factory.getOwnerContextType();
    }

    @Override
    public IClass<?>[] getParametersContextTypes() {
        if (this.deferred) {
            return this.deferredParameterTypes;
        }
        return this.factory.getParametersContextTypes();
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> execute(IExpressionNodeContext ownerContext, Object... contexts)
            throws ReflectionException {
        return this.supply(ownerContext, contexts);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> supply(IExpressionNodeContext ownerContext, Object... otherContexts)
            throws SupplyException {
        if (this.deferred) {
            return supplyDeferred(ownerContext);
        }
        return this.factory.supply(ownerContext, otherContexts);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Optional<IMethodReturn<IExpressionNode<R, S>>> supplyDeferred(IExpressionNodeContext context) {
        String methodName = this.deferredMethodName;
        IClass<?>[] paramTypes = this.deferredParameterTypes;
        ISupplier<?> ownerSupplier = this.deferredOwnerSupplier;

        IExpressionNode<R, S> node = (IExpressionNode<R, S>) new ExpressionNode<>(
                ":" + methodName,
                params -> createDeferredSupplier(ownerSupplier, methodName, paramTypes, params),
                (IClass) IClass.getClass(Object.class),
                context.parameters(),
                null);

        IClass<IExpressionNode<R, S>> nodeClass = (IClass<IExpressionNode<R, S>>) (IClass<?>) IClass.getClass(IExpressionNode.class);
        return Optional.of(SingleMethodReturn.of(node, nodeClass));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ISupplier<R> createDeferredSupplier(ISupplier<?> ownerSupplier, String methodName,
            IClass<?>[] paramTypes, Object... evaluatedParams) {
        return new ISupplier<R>() {
            private volatile IClass<R> resolvedReturnType;

            @Override
            public Optional<R> supply() throws SupplyException {
                Object owner = ownerSupplier.supply()
                        .orElseThrow(() -> new SupplyException(
                                "Owner is null for deferred method call: " + methodName));

                IClass<?> actualType = IClass.getClass(owner.getClass());
                IReflection reflection = IClass.getReflection();

                ResolvedMethod method = MethodResolver.methodByName(
                        actualType, reflection, methodName, null, paramTypes);

                // Create a fixed supplier for the owner object
                ISupplier<?> fixedOwner = new ISupplier<Object>() {
                    @Override
                    public Optional<Object> supply() { return Optional.of(owner); }
                    @Override
                    public Type getSuppliedType() { return actualType.getType(); }
                    @Override
                    public IClass<Object> getSuppliedClass() { return (IClass<Object>) (IClass<?>) actualType; }
                };

                // Wrap evaluated params as suppliers
                List<ISupplier<?>> paramSuppliers = new ArrayList<>(evaluatedParams.length);
                for (Object param : evaluatedParams) {
                    if (param instanceof ISupplier<?> s) {
                        paramSuppliers.add(s);
                    } else {
                        Object val = param;
                        paramSuppliers.add(new ISupplier<Object>() {
                            @Override
                            public Optional<Object> supply() { return Optional.ofNullable(val); }
                            @Override
                            public Type getSuppliedType() { return val == null ? Object.class : val.getClass(); }
                            @Override
                            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
                        });
                    }
                }

                MethodBinder<R> binder = new MethodBinder<>(fixedOwner, method, paramSuppliers);
                Optional<IMethodReturn<R>> result = binder.supply();
                Optional<R> value = result.flatMap(IMethodReturn::firstOptional);
                value.ifPresent(v -> {
                    if (resolvedReturnType == null) {
                        resolvedReturnType = (IClass<R>) IClass.getClass(v.getClass());
                    }
                });
                return value;
            }

            @Override
            public Type getSuppliedType() {
                return getSuppliedClass().getType();
            }

            @Override
            public IClass<R> getSuppliedClass() {
                return resolvedReturnType != null ? resolvedReturnType : (IClass<R>) IClass.getClass(Object.class);
            }
        };
    }

    @Override
    public String key() {
        if (this.deferred) {
            StringBuilder key = new StringBuilder(":" + this.deferredMethodName + "(");
            for (int i = 0; i < this.deferredParameterTypes.length; i++) {
                if (i > 0) key.append(",");
                key.append(this.deferredParameterTypes[i].getSimpleName());
            }
            key.append(")");
            return key.toString();
        }
        return this.factory.key();
    }

    @Override
    public String description() {
        if (this.deferred) {
            return "Deferred method call: " + this.deferredMethodName;
        }
        return this.factory.description();
    }

    @Override
    public String man() {
        if (this.deferred) {
            return "Deferred method call: " + this.deferredMethodName;
        }
        return this.factory.man();
    }

}
