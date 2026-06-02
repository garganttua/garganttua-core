package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Expression that adapts an {@link IContextualMethodBinder} so it can be evaluated
 * as a lazy {@link ISupplier}, supplying the current {@link IRuntimeContext} (read
 * from {@link RuntimeExpressionContext}) as the binder's invocation context.
 *
 * @param <R> the method return type
 * @param <C> the contextual invocation type
 */
public class MethodBinderExpression<R, C> implements IExpression<R, ISupplier<R>> {
    private static final Logger log = Logger.getLogger(MethodBinderExpression.class);

    private final IContextualMethodBinder<R, C> binder;
    private final String expressionReference;

    /**
     * Wraps the given contextual method binder.
     *
     * @param binder the binder to invoke during evaluation; must not be {@code null}
     */
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
                return binder.getSuppliedType();
            }

            @Override
            public IClass<R> getSuppliedClass() {
                return (IClass<R>) (IClass<?>) binder.getSuppliedClass();
            }
        };
    }

    /**
     * @return the underlying binder's executable reference string
     */
    public String getExpressionReference() {
        return expressionReference;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IClass<R> getSuppliedClass() {
        return (IClass<R>) (IClass<?>) binder.getSuppliedClass();
    }

    @Override
    public Type getSuppliedType() {
        return binder.getSuppliedType();
    }

    @Override
    public boolean isContextual() {
        return true;
    }
}
