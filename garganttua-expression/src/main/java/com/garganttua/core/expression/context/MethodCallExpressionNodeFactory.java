package com.garganttua.core.expression.context;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.supply.SupplyException;

import jakarta.annotation.Nullable;

public class MethodCallExpressionNodeFactory<R, S extends ISupplier<R>> implements IExpressionNodeFactory<R, S> {

    private ResolvedMethod resolved;
    private ExpressionNodeFactory<R, S> factory;
    private List<Boolean> nullables;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MethodCallExpressionNodeFactory(IExpressionNode<?, S> ownerNode, String methodName, Class<?>[] parameterTypes)
            throws ExpressionException {
        Objects.requireNonNull(ownerNode, "Owner node cannot be null");
        Objects.requireNonNull(methodName, "Method name cannot be null");
        Objects.requireNonNull(parameterTypes, "Parameter types array cannot be null");

        Class<?> ownerClass = ownerNode.getFinalSuppliedClass();
        ISupplier<?> ownerSupplier;

        if (ownerClass == Class.class) {
            // Static method call - evaluate the owner node to get the actual target class
            Class<?> actualClass = (Class<?>) ownerNode.evaluate().supply()
                    .orElseThrow(() -> new ExpressionException("Cannot resolve class for static method call: " + methodName));
            ownerClass = actualClass;
            ownerSupplier = new NullSupplier<>(actualClass);
        } else {
            // Instance method call
            ownerSupplier = ownerNode.evaluate();
        }

        this.resolved = MethodResolver.methodByName(ownerClass, methodName, null, parameterTypes);
        this.nullables = nullableMask(this.resolved.method());

        this.factory = new ExpressionNodeFactory(ownerSupplier, this.resolved.method().getReturnType(),
                this.resolved.method(), this.resolved.address(), nullables, Optional.of(methodName),
                Optional.of("No description available"));
    }

    private List<Boolean> nullableMask(Method method) {
        Objects.requireNonNull(method, "method cannot be null");
        Parameter[] parameters = method.getParameters();
        List<Boolean> result = new ArrayList<>(parameters.length);
        for (Parameter parameter : parameters) {
            result.add(parameter.isAnnotationPresent(Nullable.class));
        }

        return result;
    }

    @Override
    public String getExecutableReference() {
        return this.factory.getExecutableReference();
    }

    @Override
    public Set<Class<?>> dependencies() {
        return this.factory.dependencies();
    }

    @Override
    public Type getSuppliedType() {
        return this.factory.getSuppliedType();
    }

    @Override
    public Class<IExpressionNodeContext> getOwnerContextType() {
        return this.factory.getOwnerContextType();
    }

    @Override
    public Class<?>[] getParametersContextTypes() {
        return this.factory.getParametersContextTypes();
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> execute(IExpressionNodeContext ownerContext, Object... contexts)
            throws ReflectionException {
        return this.factory.execute(ownerContext, contexts);
    }

    @Override
    public Optional<IMethodReturn<IExpressionNode<R, S>>> supply(IExpressionNodeContext ownerContext, Object... otherContexts)
            throws SupplyException {
        return this.factory.supply(ownerContext, otherContexts);
    }

    @Override
    public String key() {
        return this.factory.key();
    }

    @Override
    public String description() {
        return this.factory.description();
    }

    @Override
    public String man() {
        return this.factory.man();
    }

}
