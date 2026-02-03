package com.garganttua.core.expression.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ConstructorBinder;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

import jakarta.annotation.Nullable;

public class ConstructorCallExpressionNodeFactory<R, S extends ISupplier<R>> implements IExpressionNodeFactory<R, S> {

    private final Constructor<R> constructor;
    private final Class<R> targetClass;
    private final List<Boolean> nullables;

    @SuppressWarnings("unchecked")
    public ConstructorCallExpressionNodeFactory(IExpressionNode<?, ?> classNode, Class<?>[] parameterTypes)
            throws ExpressionException {
        Objects.requireNonNull(classNode, "Class node cannot be null");
        Objects.requireNonNull(parameterTypes, "Parameter types array cannot be null");

        // Evaluate the class node to get the actual target class
        Class<?> actualClass = (Class<?>) classNode.evaluate().supply()
                .orElseThrow(() -> new ExpressionException("Cannot resolve class for constructor call"));

        this.targetClass = (Class<R>) actualClass;

        try {
            this.constructor = (Constructor<R>) findConstructor(actualClass, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ExpressionException(
                    "No constructor found for class " + actualClass.getName()
                            + " with parameter types " + java.util.Arrays.toString(parameterTypes));
        }

        this.nullables = nullableMask(this.constructor);
    }

    private Constructor<?> findConstructor(Class<?> clazz, Class<?>[] parameterTypes) throws NoSuchMethodException {
        // Try exact match first
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            // Try assignable match
            for (Constructor<?> c : clazz.getConstructors()) {
                Class<?>[] ctorParams = c.getParameterTypes();
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

    private List<Boolean> nullableMask(Constructor<?> ctor) {
        Parameter[] parameters = ctor.getParameters();
        List<Boolean> result = new ArrayList<>(parameters.length);
        for (Parameter parameter : parameters) {
            result.add(parameter.isAnnotationPresent(Nullable.class));
        }
        return result;
    }

    @Override
    public String key() {
        StringBuilder key = new StringBuilder(":");
        key.append("(");
        key.append(targetClass.getSimpleName());
        Class<?>[] paramTypes = constructor.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            key.append(",");
            key.append(paramType.getSimpleName());
        }
        key.append(")");
        return key.toString();
    }

    @Override
    public String description() {
        return "Constructor for " + targetClass.getSimpleName();
    }

    @Override
    public String man() {
        return "Constructor: " + targetClass.getName() + "(" +
                java.util.Arrays.stream(constructor.getParameterTypes())
                        .map(Class::getSimpleName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("")
                + ")";
    }

    @Override
    public String getExecutableReference() {
        return targetClass.getName() + ".<init>";
    }

    @Override
    public Set<Class<?>> dependencies() {
        return Set.of();
    }

    @Override
    public Type getSuppliedType() {
        return targetClass;
    }

    @Override
    public Class<IExpressionNodeContext> getOwnerContextType() {
        return IExpressionNodeContext.class;
    }

    @Override
    public Class<?>[] getParametersContextTypes() {
        return constructor.getParameterTypes();
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> execute(IExpressionNodeContext ownerContext,
            Object... contexts) throws ReflectionException {
        return this.supply(ownerContext, contexts);
    }

    @SuppressWarnings("unchecked")
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
                            supplier = new FixedSupplierBuilder<>(params[i]).build();
                        }
                        boolean nullable = this.nullables.get(i);
                        paramSuppliers.add(new NullableSupplier<>(supplier, nullable));
                    }

                    ConstructorBinder<R> binder = new ConstructorBinder<>(targetClass, constructor, paramSuppliers);
                    return new ConstructorUnwrappingSupplier<>(binder, targetClass);
                },
                targetClass,
                context.parameters());

        return Optional.of(SingleMethodReturn.of(node));
    }

    private static class ConstructorUnwrappingSupplier<T> implements ISupplier<T> {
        private final ConstructorBinder<T> binder;
        private final Class<T> returnType;

        ConstructorUnwrappingSupplier(ConstructorBinder<T> binder, Class<T> returnType) {
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
            return returnType;
        }
    }
}
