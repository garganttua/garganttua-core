package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

public class LowerOrEqualsCondition<T> implements ICondition {

    private final ISupplier<T> supplier1;
    private final ISupplier<T> supplier2;

    public LowerOrEqualsCondition(ISupplier<T> supplier1, ISupplier<T> supplier2) {
        this.supplier1 = Objects.requireNonNull(supplier1);
        this.supplier2 = Objects.requireNonNull(supplier2);
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        Object a = this.supplier1.supply().orElseThrow(() -> new ConditionException("Supplier 1 supplied empty value"));
        Object b = this.supplier2.supply().orElseThrow(() -> new ConditionException("Supplier 2 supplied empty value"));
        return new FixedSupplier<>(lowerOrEquals(a, b));
    }

    @Expression(name = "lowerOrEquals", description = "Checks if first argument is lower than or equal to second")
    public static boolean lowerOrEquals(Object a, Object b) {
        if (a == null || b == null) {
            return false;
        }
        return ComparisonHelper.compare(a, b) <= 0;
    }

    @Expression(name = "lowerOrEquals", description = "Checks if first int argument is lower than or equal to second int")
    public static boolean lowerOrEquals(int a, int b) {
        return a <= b;
    }

    @Expression(name = "lowerOrEquals", description = "Checks if first long argument is lower than or equal to second long")
    public static boolean lowerOrEquals(long a, long b) {
        return a <= b;
    }

    @Expression(name = "lowerOrEquals", description = "Checks if first double argument is lower than or equal to second double")
    public static boolean lowerOrEquals(double a, double b) {
        return a <= b;
    }

    @Expression(name = "lowerOrEquals", description = "Checks if first Object argument is lower than or equal to second int")
    public static boolean lowerOrEquals(Object a, int b) {
        if (a == null) return false;
        return ComparisonHelper.compare(a, b) <= 0;
    }

    @Expression(name = "lowerOrEquals", description = "Checks if first int argument is lower than or equal to second Object")
    public static boolean lowerOrEquals(int a, Object b) {
        if (b == null) return false;
        return ComparisonHelper.compare(a, b) <= 0;
    }
}
