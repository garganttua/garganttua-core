package com.garganttua.core.expression.context;

import java.lang.reflect.Type;
import java.util.ArrayList;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
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
        return this.factory.getExecutableReference();
    }

    @Override
    public Set<IClass<?>> dependencies() {
        return this.factory.dependencies();
    }

    @Override
    public Type getSuppliedType() {
        return this.factory.getSuppliedType();
    }

    @Override
    public IClass<IMethodReturn<IExpressionNode<R, S>>> getSuppliedClass() {
        return this.factory.getSuppliedClass();
    }

    @Override
    public IClass<IExpressionNodeContext> getOwnerContextType() {
        return this.factory.getOwnerContextType();
    }

    @Override
    public IClass<?>[] getParametersContextTypes() {
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
