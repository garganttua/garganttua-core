package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

import com.garganttua.core.reflection.annotations.Reflected;
@Reflected(queryAllDeclaredMethods = true)
public class NotNullCondition implements ICondition {
    private static final IDiagnostic log = Diagnostics.of(NotNullCondition.class);

    private ISupplier<?> supplier;

    public NotNullCondition(ISupplier<?> supplier) {
        log.trace("Entering NotNullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier cannot be null");
        log.trace("Exiting NotNullCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for NotNullCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating NOT NULL condition - negation of NULL condition");
                boolean result = notNull(supplier.supply().orElse(null));
                log.debug("NOT NULL condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "notNull", description = "Checks if an object is not null; an empty Optional counts as null")
    public static boolean notNull(Object obj) {
        // Optional-aware: an empty Optional means "no value", so it must read as
        // null here. Without this, guards built on suppliers that return
        // Optional.ofNullable(...) (e.g. request-arg lookups) are never skipped
        // because Optional.empty() is itself a non-null reference.
        boolean result = obj instanceof Optional<?> opt ? opt.isPresent() : obj != null;
        log.debug("NOT NULL condition result: {}", result);
        return result;
    }

}
