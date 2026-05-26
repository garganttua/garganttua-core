package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

public class NullCondition implements ICondition {
    private static final IDiagnostic log = Diagnostics.of(NullCondition.class);

    private ISupplier<?> supplier;

    public NullCondition(ISupplier<?> supplier) {
        log.trace("Entering NullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.trace("Exiting NullCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for NullCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating NULL condition - checking if supplier returns null/empty");
                Boolean result = Null(supplier.supply().orElse(null));
                log.debug("NULL condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "null", description = "Checks if an object is not null")
    public static boolean Null(Object obj) {
        boolean result = obj == null;
        log.debug("NULL condition result: {}", result);

        return result;
    }

}
