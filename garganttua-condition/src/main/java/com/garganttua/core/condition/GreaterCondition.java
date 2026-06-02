package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Ordering condition: evaluates to {@code true} when the first supplied value is
 * strictly greater than the second (see {@link ComparisonHelper#compare}).
 *
 * @param <T> the supplied value type
 */
@Reflected(queryAllDeclaredMethods = true)
public class GreaterCondition<T> implements ICondition {

    private final ISupplier<T> supplier1;
    private final ISupplier<T> supplier2;

    /**
     * Creates a greater-than condition over two suppliers.
     *
     * @param supplier1 supplier of the first value; must not be {@code null}
     * @param supplier2 supplier of the second value; must not be {@code null}
     */
    public GreaterCondition(ISupplier<T> supplier1, ISupplier<T> supplier2) {
        this.supplier1 = Objects.requireNonNull(supplier1);
        this.supplier2 = Objects.requireNonNull(supplier2);
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        Object a = this.supplier1.supply().orElseThrow(() -> new ConditionException("Supplier 1 supplied empty value"));
        Object b = this.supplier2.supply().orElseThrow(() -> new ConditionException("Supplier 2 supplied empty value"));
        return new FixedSupplier<>(greater(a, b), IClass.getClass(Boolean.class));
    }

    /**
     * Checks whether {@code a} is greater than {@code b}, unwrapping
     * {@link java.util.Optional} operands; a {@code null} operand yields {@code false}.
     *
     * @param a first operand, may be {@code null}
     * @param b second operand, may be {@code null}
     * @return {@code true} when {@code a > b}
     */
    @Expression(name = "greater", description = "Checks if first argument is greater than second")
    public static boolean greater(Object a, Object b) {
        a = ComparisonHelper.unwrapOptional(a);
        b = ComparisonHelper.unwrapOptional(b);
        if (a == null || b == null) {
            return false;
        }
        return ComparisonHelper.compare(a, b) > 0;
    }

    /**
     * Primitive {@code int} overload of {@link #greater(Object, Object)}.
     *
     * @param a first operand
     * @param b second operand
     * @return {@code true} when {@code a > b}
     */
    @Expression(name = "greater", description = "Checks if first int argument is greater than second int")
    public static boolean greater(int a, int b) {
        return a > b;
    }

    /**
     * Primitive {@code long} overload of {@link #greater(Object, Object)}.
     *
     * @param a first operand
     * @param b second operand
     * @return {@code true} when {@code a > b}
     */
    @Expression(name = "greater", description = "Checks if first long argument is greater than second long")
    public static boolean greater(long a, long b) {
        return a > b;
    }

    /**
     * Primitive {@code double} overload of {@link #greater(Object, Object)}.
     *
     * @param a first operand
     * @param b second operand
     * @return {@code true} when {@code a > b}
     */
    @Expression(name = "greater", description = "Checks if first double argument is greater than second double")
    public static boolean greater(double a, double b) {
        return a > b;
    }

    /**
     * Mixed {@code Object}/{@code int} overload of {@link #greater(Object, Object)};
     * a {@code null} first operand yields {@code false}.
     *
     * @param a first operand, may be {@code null}
     * @param b second operand
     * @return {@code true} when {@code a > b}
     */
    @Expression(name = "greater", description = "Checks if first Object argument is greater than second int")
    public static boolean greater(Object a, int b) {
        a = ComparisonHelper.unwrapOptional(a);
        if (a == null) return false;
        return ComparisonHelper.compare(a, b) > 0;
    }

    /**
     * Mixed {@code int}/{@code Object} overload of {@link #greater(Object, Object)};
     * a {@code null} second operand yields {@code false}.
     *
     * @param a first operand
     * @param b second operand, may be {@code null}
     * @return {@code true} when {@code a > b}
     */
    @Expression(name = "greater", description = "Checks if first int argument is greater than second Object")
    public static boolean greater(int a, Object b) {
        b = ComparisonHelper.unwrapOptional(b);
        if (b == null) return false;
        return ComparisonHelper.compare(a, b) > 0;
    }
}
