package com.garganttua.core.script.context;

import java.lang.reflect.Type;
import java.util.Optional;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.context.ExpressionVariableContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Expression decorator that sets up {@link ExpressionVariableContext} with a
 * {@link ScriptVariableResolver} before evaluation and clears it after.
 *
 * <p>
 * This bridges the expression engine's variable resolution to the runtime context.
 * The {@link com.garganttua.core.runtime.RuntimeExpressionContext} is already set
 * by {@link com.garganttua.core.runtime.RuntimeStepMethodBinder} — this wrapper
 * adds the script-specific variable resolver on top.
 * </p>
 *
 * @param <R> the result type
 * @since 2.0.0-ALPHA01
 */
public class ScriptExpressionWrapper<R> implements IExpression<R, ISupplier<R>> {

    private static final ScriptVariableResolver RESOLVER = new ScriptVariableResolver();

    private final IExpression<R, ? extends ISupplier<R>> inner;

    public ScriptExpressionWrapper(IExpression<R, ? extends ISupplier<R>> inner) {
        this.inner = inner;
    }

    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        return new ISupplier<>() {
            @Override
            public Optional<R> supply() throws SupplyException {
                ExpressionVariableContext.set(RESOLVER);
                try {
                    return inner.evaluate().supply();
                } catch (ExpressionException e) {
                    throw new SupplyException(e);
                } finally {
                    ExpressionVariableContext.clear();
                }
            }

            @Override
            public Type getSuppliedType() {
                return inner.getSuppliedType();
            }

            @Override
            public IClass<R> getSuppliedClass() {
                return inner.getSuppliedClass();
            }
        };
    }

    @Override
    public Type getSuppliedType() {
        return inner.getSuppliedType();
    }

    @Override
    public IClass<R> getSuppliedClass() {
        return inner.getSuppliedClass();
    }

    @Override
    public boolean isContextual() {
        return inner.isContextual();
    }
}
