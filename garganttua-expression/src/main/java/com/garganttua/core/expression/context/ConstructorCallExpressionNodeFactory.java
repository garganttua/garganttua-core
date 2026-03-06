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
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ConstructorBinder;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

import jakarta.annotation.Nullable;

public class ConstructorCallExpressionNodeFactory<R, S extends ISupplier<R>> implements IExpressionNodeFactory<R, S> {

    private final IConstructor<R> constructor;
    private final IClass<R> targetIClass;
    private final List<Boolean> nullables;

    public ConstructorCallExpressionNodeFactory(IExpressionNode<?, ?> classNode, IClass<?>[] parameterTypes)
            throws ExpressionException {
        Objects.requireNonNull(classNode, "Class node cannot be null");
        Objects.requireNonNull(parameterTypes, "Parameter types array cannot be null");

        // Evaluate the class node to get the actual target class
        Object classResult = classNode.evaluate().supply()
                .orElseThrow(() -> new ExpressionException("Cannot resolve class for constructor call"));

        if (classResult instanceof IClass<?> ic) {
            this.targetIClass = (IClass<R>) ic;
        } else if (classResult instanceof Class<?> c) {
            this.targetIClass = (IClass<R>) IClass.getClass(c);
        } else {
            throw new ExpressionException("Cannot resolve class for constructor call: unexpected type " + classResult.getClass().getName());
        }

        try {
            this.constructor = (IConstructor<R>) findConstructor(this.targetIClass, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ExpressionException(
                    "No constructor found for class " + this.targetIClass.getName()
                            + " with parameter types " + java.util.Arrays.toString(parameterTypes));
        }

        this.nullables = nullableMask(this.constructor);
    }

    private IConstructor<?> findConstructor(IClass<?> clazz, IClass<?>[] parameterTypes) throws NoSuchMethodException {
        // Try exact match first
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            // Try assignable match
            for (IConstructor<?> c : clazz.getConstructors()) {
                IClass<?>[] ctorParams = c.getParameterTypes();
                if (ctorParams.length != parameterTypes.length) continue;
                boolean matches = true;
                for (int i = 0; i < ctorParams.length; i++) {
                    if (!ctorParams[i].isAssignableFrom(parameterTypes[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) return c;
            }
            throw e;
        }
    }

    private List<Boolean> nullableMask(IConstructor<?> ctor) {
        IClass<Nullable> nullableClass = IClass.getClass(Nullable.class);
        IParameter[] parameters = ctor.getParameters();
        List<Boolean> result = new ArrayList<>(parameters.length);
        for (IParameter parameter : parameters) {
            result.add(parameter.isAnnotationPresent(nullableClass));
        }
        return result;
    }

    @Override
    public String key() {
        StringBuilder key = new StringBuilder(":");
        key.append("(");
        key.append(targetIClass.getSimpleName());
        IClass<?>[] paramTypes = constructor.getParameterTypes();
        for (IClass<?> paramType : paramTypes) {
            key.append(",");
            key.append(paramType.getSimpleName());
        }
        key.append(")");
        return key.toString();
    }

    @Override
    public String description() {
        return "Constructor for " + targetIClass.getSimpleName();
    }

    @Override
    public String man() {
        return "Constructor: " + targetIClass.getName() + "(" +
                java.util.Arrays.stream(constructor.getParameterTypes())
                        .map(IClass::getSimpleName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("")
                + ")";
    }

    @Override
    public String getExecutableReference() {
        return targetIClass.getName() + ".<init>";
    }

    @Override
    public Set<IClass<?>> dependencies() {
        return Set.of();
    }

    @Override
    public Type getSuppliedType() {
        return targetIClass.getType();
    }

    @Override
    public IClass<IMethodReturn<IExpressionNode<R, S>>> getSuppliedClass() {
        return (IClass<IMethodReturn<IExpressionNode<R, S>>>) (IClass<?>) IClass.getClass(IMethodReturn.class);
    }

    @Override
    public IClass<IExpressionNodeContext> getOwnerContextType() {
        return IClass.getClass(IExpressionNodeContext.class);
    }

    @Override
    public IClass<?>[] getParametersContextTypes() {
        return constructor.getParameterTypes();
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> execute(IExpressionNodeContext ownerContext,
            Object... contexts) throws ReflectionException {
        return this.supply(ownerContext, contexts);
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> supply(IExpressionNodeContext context,
            Object... otherContexts) throws SupplyException {

        IExpressionNode<R, S> node = (IExpressionNode<R, S>) new ExpressionNode<>(
                getExecutableReference(),
                (params) -> {
                    List<ISupplier<?>> paramSuppliers = new ArrayList<>(params.length);
                    for (int i = 0; i < params.length; i++) {
                        ISupplier<?> supplier;
                        if (params[i] instanceof ISupplier<?>) {
                            supplier = (ISupplier<?>) params[i];
                        } else {
                            supplier = FixedSupplierBuilder.of(params[i]).build();
                        }
                        boolean nullable = this.nullables.get(i);
                        paramSuppliers.add(new NullableSupplier<>(supplier, nullable));
                    }

                    ConstructorBinder<R> binder = new ConstructorBinder<>(targetIClass, constructor, paramSuppliers);
                    return new ConstructorUnwrappingSupplier<>(binder, targetIClass);
                },
                targetIClass,
                context.parameters());

        IClass<IExpressionNode<R, S>> nodeClass = (IClass<IExpressionNode<R, S>>) (IClass<?>) IClass.getClass(IExpressionNode.class);
        return Optional.of(SingleMethodReturn.of(node, nodeClass));
    }

    private static class ConstructorUnwrappingSupplier<T> implements ISupplier<T> {
        private final ConstructorBinder<T> binder;
        private final IClass<T> returnType;

        ConstructorUnwrappingSupplier(ConstructorBinder<T> binder, IClass<T> returnType) {
            this.binder = binder;
            this.returnType = returnType;
        }

        @Override
        public Optional<T> supply() throws SupplyException {
            return binder.supply()
                    .flatMap(methodReturn -> {
                        if (methodReturn.hasException()) {
                            throw new RuntimeException(new SupplyException(
                                    "Constructor invocation failed", methodReturn.getException()));
                        }
                        return methodReturn.firstOptional();
                    });
        }

        @Override
        public Type getSuppliedType() {
            return returnType.getType();
        }

        @Override
        public IClass<T> getSuppliedClass() {
            return returnType;
        }
    }
}
