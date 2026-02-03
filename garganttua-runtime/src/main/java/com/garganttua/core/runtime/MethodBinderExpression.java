package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodBinderExpression<R, C> implements IExpression<R, ISupplier<R>> {

    private final IContextualMethodBinder<R, C> binder;
    private final String expressionReference;

    public MethodBinderExpression(IContextualMethodBinder<R, C> binder) {
        this.binder = Objects.requireNonNull(binder, "Binder cannot be null");
        this.expressionReference = binder.getExecutableReference();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        return new ISupplier<R>() {
            @Override
            public Optional<R> supply() throws SupplyException {
                C context = (C) RuntimeExpressionContext.get();
                try {
                    Optional<IMethodReturn<R>> result = binder.execute(context);
                    if (result.isPresent()) {
                        IMethodReturn<R> methodReturn = result.get();
                        if (methodReturn.hasException()) {
                            throw new SupplyException(new RuntimeException("Method threw exception", methodReturn.getException()));
                        }
                        return Optional.ofNullable(methodReturn.single());
                    }
                    return Optional.empty();
                } catch (SupplyException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SupplyException(e);
                }
            }

            @Override
            public Type getSuppliedType() {
                return binder.getSuppliedClass();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<R> getSuppliedClass() {
                return (Class<R>) binder.getSuppliedClass();
            }
        };
    }

    public String getExpressionReference() {
        return expressionReference;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<R> getSuppliedClass() {
        return (Class<R>) binder.getSuppliedClass();
    }

    @Override
    public Type getSuppliedType() {
        return binder.getSuppliedClass();
    }

    @Override
    public boolean isContextual() {
        return true;
    }
}
