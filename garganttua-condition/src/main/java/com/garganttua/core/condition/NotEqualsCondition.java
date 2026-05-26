package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

public class NotEqualsCondition<T> implements ICondition {
    private static final IDiagnostic log = Diagnostics.of(NotEqualsCondition.class);

    private ISupplier<T> supplier1;
    private ISupplier<T> supplier2;

    public NotEqualsCondition(ISupplier<T> supplier1, ISupplier<T> supplier2) {
        log.trace("Entering NotEqualsCondition constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 cannot be null");

        log.trace("Exiting NotEqualsCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for NotEqualsCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating NOT EQUALS condition - negation of EQUALS condition");
                boolean result = notEquals(supplier1.supply().orElse(null), supplier2.supply().orElse(null));
                log.debug("NOT EQUALS condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "notEquals", description = "Checks if two objects are not equal")
    public static boolean notEquals(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null) {
            return false;
        }
        if (!obj1.getClass().equals(obj2.getClass())) {
            log.error("Type mismatch: {} VS {}",
                    obj1.getClass().getSimpleName(),
                    obj2.getClass().getSimpleName());
            return true;
        }
        boolean equalsResult = Objects.equals(obj1, obj2);
        log.debug("EQUALS condition result: {}", equalsResult);

        boolean result = !equalsResult;
        return result;
    }

}
