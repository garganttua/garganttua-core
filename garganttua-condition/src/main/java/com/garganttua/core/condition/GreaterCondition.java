package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

public class GreaterCondition<T> implements ICondition {

    private final ISupplier<T> supplier1;
    private final ISupplier<T> supplier2;

    public GreaterCondition(ISupplier<T> supplier1, ISupplier<T> supplier2) {
        this.supplier1 = Objects.requireNonNull(supplier1);
        this.supplier2 = Objects.requireNonNull(supplier2);
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        Object a = this.supplier1.supply().orElseThrow(() -> new ConditionException("Supplier 1 supplied empty value"));
        Object b = this.supplier2.supply().orElseThrow(() -> new ConditionException("Supplier 2 supplied empty value"));
        return new FixedSupplier<>(greater(a, b));
    }

    @Expression(name = "greater", description = "Checks if first argument is greater than second")
    public static boolean greater(Object a, Object b) {
        if (a == null || b == null) {
            return false;
        }
        return ComparisonHelper.compare(a, b) > 0;
    }

    @Expression(name = "greater", description = "Checks if first int argument is greater than second int")
    public static boolean greater(int a, int b) {
        return a > b;
    }

    @Expression(name = "greater", description = "Checks if first long argument is greater than second long")
    public static boolean greater(long a, long b) {
        return a > b;
    }

    @Expression(name = "greater", description = "Checks if first double argument is greater than second double")
    public static boolean greater(double a, double b) {
        return a > b;
    }

    @Expression(name = "greater", description = "Checks if first Object argument is greater than second int")
    public static boolean greater(Object a, int b) {
        if (a == null) return false;
        return ComparisonHelper.compare(a, b) > 0;
    }

    @Expression(name = "greater", description = "Checks if first int argument is greater than second Object")
    public static boolean greater(int a, Object b) {
        if (b == null) return false;
        return ComparisonHelper.compare(a, b) > 0;
    }
}
