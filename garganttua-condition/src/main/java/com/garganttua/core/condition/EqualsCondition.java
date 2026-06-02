package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

import com.garganttua.core.reflection.annotations.Reflected;
@Reflected(queryAllDeclaredMethods = true)
public class EqualsCondition<T> implements ICondition {
    private static final Logger log = Logger.getLogger(EqualsCondition.class);

    private ISupplier<T> supplier1;
    private ISupplier<T> supplier2;

    public EqualsCondition(ISupplier<T> supplier1, ISupplier<T> supplier2) {
        log.trace("Entering EqualsCondition constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 cannot be null");
        log.trace("Exiting EqualsCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for EqualsCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating EQUALS condition - comparing two supplied values");
                Object val1 = supplier1.supply().orElseThrow(() -> {
                    log.error("Supplier 1 supplied empty value");
                    return new ConditionException("Supplier 1 supplied empty value");
                });
                Object val2 = supplier2.supply().orElseThrow(() -> {
                    log.error("Supplier 2 supplied empty value");
                    return new ConditionException("Supplier 2 supplied empty value");
                });
                boolean result = EqualsCondition.equals(val1, val2);
                log.debug("EQUALS condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "equals", description = "Checks if two objects are equal")
    public static boolean equals(Object obj1, Object obj2) {
        log.trace("Entering static equals() method");
        obj1 = ComparisonHelper.unwrapOptional(obj1);
        obj2 = ComparisonHelper.unwrapOptional(obj2);
        if( obj1 == null || obj2 == null ) {
            return false;
        }
        if (!obj1.getClass().equals(obj2.getClass())) {
            log.error("Type mismatch: {} VS {}",
                obj1.getClass().getSimpleName(),
                obj2.getClass().getSimpleName());
            return false;
        }
        boolean result = Objects.equals(obj1, obj2);
        log.debug("Equality check result for objects {} and {}: {}", obj1, obj2, result);
        log.trace("Exiting static equals() method with result: {}", result);
        return result;
    }

}
