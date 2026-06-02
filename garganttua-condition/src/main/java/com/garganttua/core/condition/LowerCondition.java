package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Ordering condition: evaluates to {@code true} when the first supplied value is
 * strictly lower than the second (see {@link ComparisonHelper#compare}).
 *
 * @param <T> the supplied value type
 */
@Reflected(queryAllDeclaredMethods = true)
public class LowerCondition<T> implements ICondition {

    private final ISupplier<T> supplier1;
    private final ISupplier<T> supplier2;

    /**
     * Creates a lower-than condition over two suppliers.
     *
     * @param supplier1 supplier of the first value; must not be {@code null}
     * @param supplier2 supplier of the second value; must not be {@code null}
     */
    public LowerCondition(ISupplier<T> supplier1, ISupplier<T> supplier2) {
        this.supplier1 = Objects.requireNonNull(supplier1);
        this.supplier2 = Objects.requireNonNull(supplier2);
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        Object a = this.supplier1.supply().orElseThrow(() -> new ConditionException("Supplier 1 supplied empty value"));
        Object b = this.supplier2.supply().orElseThrow(() -> new ConditionException("Supplier 2 supplied empty value"));
        return new FixedSupplier<>(lower(a, b), IClass.getClass(Boolean.class));
    }

    /**
     * Checks whether {@code a} is lower than {@code b}, unwrapping
     * {@link java.util.Optional} operands; a {@code null} operand yields {@code false}.
     *
     * @param a first operand, may be {@code null}
     * @param b second operand, may be {@code null}
     * @return {@code true} when {@code a < b}
     */
    @Expression(name = "lower", description = "Checks if first argument is lower than second")
    public static boolean lower(Object a, Object b) {
        a = ComparisonHelper.unwrapOptional(a);
        b = ComparisonHelper.unwrapOptional(b);
        if (a == null || b == null) {
            return false;
        }
        return ComparisonHelper.compare(a, b) < 0;
    }

    /**
     * Primitive {@code int} overload of {@link #lower(Object, Object)}.
     *
     * @param a first operand
     * @param b second operand
     * @return {@code true} when {@code a < b}
     */
    @Expression(name = "lower", description = "Checks if first int argument is lower than second int")
    public static boolean lower(int a, int b) {
        return a < b;
    }

    /**
     * Primitive {@code long} overload of {@link #lower(Object, Object)}.
     *
     * @param a first operand
     * @param b second operand
     * @return {@code true} when {@code a < b}
     */
    @Expression(name = "lower", description = "Checks if first long argument is lower than second long")
    public static boolean lower(long a, long b) {
        return a < b;
    }

    /**
     * Primitive {@code double} overload of {@link #lower(Object, Object)}.
     *
     * @param a first operand
     * @param b second operand
     * @return {@code true} when {@code a < b}
     */
    @Expression(name = "lower", description = "Checks if first double argument is lower than second double")
    public static boolean lower(double a, double b) {
        return a < b;
    }

    /**
     * Mixed {@code Object}/{@code int} overload of {@link #lower(Object, Object)};
     * a {@code null} first operand yields {@code false}.
     *
     * @param a first operand, may be {@code null}
     * @param b second operand
     * @return {@code true} when {@code a < b}
     */
    @Expression(name = "lower", description = "Checks if first Object argument is lower than second int")
    public static boolean lower(Object a, int b) {
        a = ComparisonHelper.unwrapOptional(a);
        if (a == null) return false;
        return ComparisonHelper.compare(a, b) < 0;
    }

    /**
     * Mixed {@code int}/{@code Object} overload of {@link #lower(Object, Object)};
     * a {@code null} second operand yields {@code false}.
     *
     * @param a first operand
     * @param b second operand, may be {@code null}
     * @return {@code true} when {@code a < b}
     */
    @Expression(name = "lower", description = "Checks if first int argument is lower than second Object")
    public static boolean lower(int a, Object b) {
        b = ComparisonHelper.unwrapOptional(b);
        if (b == null) return false;
        return ComparisonHelper.compare(a, b) < 0;
    }
}
