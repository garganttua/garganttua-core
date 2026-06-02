package com.garganttua.core.condition;

import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.dsl.AndConditionBuilder;
import com.garganttua.core.condition.dsl.CustomConditionBuilder;
import com.garganttua.core.condition.dsl.CustomExtractedConditionBuilder;
import com.garganttua.core.condition.dsl.EqualsConditionBuilder;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.condition.dsl.NandConditionBuilder;
import com.garganttua.core.condition.dsl.NorConditionBuilder;
import com.garganttua.core.condition.dsl.NotEqualsConditionBuilder;
import com.garganttua.core.condition.dsl.NotNullConditionBuilder;
import com.garganttua.core.condition.dsl.NullConditionBuilder;
import com.garganttua.core.condition.dsl.OrConditionBuilder;
import com.garganttua.core.condition.dsl.XorConditionBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Static factory of {@link IConditionBuilder} instances — the fluent entry point
 * for composing boolean conditions (logical operators, equality, null checks and
 * custom predicates).
 */
public class Conditions {
    private static final Logger log = Logger.getLogger(Conditions.class);

    private Conditions() {

    }

    /**
     * Builds a logical AND over the given condition builders.
     *
     * @param conditions the conditions to combine
     * @return an AND condition builder
     */
    public static IConditionBuilder and(IConditionBuilder... conditions) {
        log.trace("Creating AND condition builder with {} conditions", conditions.length);
        return new AndConditionBuilder(conditions);
    }

    /**
     * Builds a logical OR over the given condition builders.
     *
     * @param conditions the conditions to combine
     * @return an OR condition builder
     */
    public static IConditionBuilder or(IConditionBuilder... conditions) {
        log.trace("Creating OR condition builder with {} conditions", conditions.length);
        return new OrConditionBuilder(conditions);
    }

    /**
     * Builds a logical XOR over the given condition builders.
     *
     * @param conditions the conditions to combine
     * @return an XOR condition builder
     */
    public static IConditionBuilder xor(IConditionBuilder... conditions) {
        log.trace("Creating XOR condition builder with {} conditions", conditions.length);
        return new XorConditionBuilder(conditions);
    }

    /**
     * Builds a logical NAND (negated AND) over the given condition builders.
     *
     * @param conditions the conditions to combine
     * @return a NAND condition builder
     */
    public static IConditionBuilder nand(IConditionBuilder... conditions) {
        log.trace("Creating NAND condition builder with {} conditions", conditions.length);
        return new NandConditionBuilder(conditions);
    }

    /**
     * Builds a logical NOR (negated OR) over the given condition builders.
     *
     * @param conditions the conditions to combine
     * @return a NOR condition builder
     */
    public static IConditionBuilder nor(IConditionBuilder... conditions) {
        log.trace("Creating NOR condition builder with {} conditions", conditions.length);
        return new NorConditionBuilder(conditions);
    }

    //
    // GENERICS
    //

    /**
     * Builds an inequality condition comparing the values produced by two suppliers.
     *
     * @param supplier1 supplier of the first value
     * @param supplier2 supplier of the second value
     * @param <T>       the supplied value type
     * @return a not-equals condition builder
     */
    public static <T> IConditionBuilder notEquals(ISupplierBuilder<T, ISupplier<T>> supplier1,
            ISupplierBuilder<T, ISupplier<T>> supplier2) {
        log.trace("Creating NOT EQUALS condition builder with suppliers");
        return new NotEqualsConditionBuilder<T>(supplier1, supplier2);
    }

    /**
     * Builds an inequality condition comparing two fixed objects.
     *
     * @param object1 the first value
     * @param object2 the second value
     * @param <T>     the value type
     * @return a not-equals condition builder
     */
    public static <T> IConditionBuilder notEquals(T object1, T object2) {
        log.trace("Creating NOT EQUALS condition builder with fixed objects");
        return new NotEqualsConditionBuilder<T>(FixedSupplierBuilder.of(object1),
                FixedSupplierBuilder.of(object2));
    }

    /**
     * Builds an equality condition comparing the values produced by two suppliers.
     *
     * @param supplier1 supplier of the first value
     * @param supplier2 supplier of the second value
     * @param <T>       the supplied value type
     * @return an equals condition builder
     */
    public static <T> IConditionBuilder equals(ISupplierBuilder<T, ISupplier<T>> supplier1,
            ISupplierBuilder<T, ISupplier<T>> supplier2) {
        log.trace("Creating EQUALS condition builder with suppliers");
        return new EqualsConditionBuilder<T>(supplier1, supplier2);
    }

    /**
     * Builds an equality condition comparing two fixed objects.
     *
     * @param object1 the first value
     * @param object2 the second value
     * @param <T>     the value type
     * @return an equals condition builder
     */
    public static <T> IConditionBuilder equals(T object1, T object2) {
        log.trace("Creating EQUALS condition builder with fixed objects");
        return new EqualsConditionBuilder<T>(FixedSupplierBuilder.of(object1),
                FixedSupplierBuilder.of(object2));
    }

    /**
     * Builds a not-null condition over the value produced by a supplier.
     *
     * @param supplier supplier of the value to test
     * @param <T>      the supplied value type
     * @return a not-null condition builder
     */
    public static <T> IConditionBuilder isNotNull(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.trace("Creating NOT NULL condition builder with supplier");
        return new NotNullConditionBuilder<T>(supplier);
    }

    /**
     * Builds a not-null condition over a fixed object.
     *
     * @param object the value to test
     * @param <T>    the value type
     * @return a not-null condition builder
     */
    public static <T> IConditionBuilder isNotNull(T object) {
        log.trace("Creating NOT NULL condition builder with fixed object");
        return new NotNullConditionBuilder<>(FixedSupplierBuilder.of(object));
    }

    /**
     * Builds a null condition over the value produced by a supplier.
     *
     * @param supplier supplier of the value to test
     * @param <T>      the supplied value type
     * @return a null condition builder
     */
    public static <T> IConditionBuilder isNull(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.trace("Creating NULL condition builder with supplier");
        return new NullConditionBuilder<T>(supplier);
    }

    /**
     * Builds a null condition over a fixed object.
     *
     * @param object the value to test
     * @param <T>    the value type
     * @return a null condition builder
     */
    public static <T> IConditionBuilder isNull(T object) {
        log.trace("Creating NULL condition builder with fixed object");
        return new NullConditionBuilder<>(FixedSupplierBuilder.of(object));
    }

    /**
     * Builds a custom condition that extracts a value from the supplied object
     * and tests it against a predicate.
     *
     * @param builder   supplier of the source value
     * @param extractor function deriving the value to test
     * @param predicate predicate applied to the extracted value
     * @param <T>       the supplied source type
     * @param <R>       the extracted value type
     * @return a custom extracted condition builder
     */
    public static <T, R> CustomExtractedConditionBuilder<T, R> custom(
            ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.trace("Creating CUSTOM EXTRACTED condition builder");
        return new CustomExtractedConditionBuilder<>(builder, extractor, predicate);
    }

    /**
     * Builds a custom condition that tests the supplied value against a predicate.
     *
     * @param builder   supplier of the value to test
     * @param predicate predicate applied to the supplied value
     * @param <T>       the supplied value type
     * @return a custom condition builder
     */
    public static <T> CustomConditionBuilder<T> custom(
            ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Predicate<T> predicate) {
        log.trace("Creating CUSTOM condition builder");
        return new CustomConditionBuilder<>(builder, predicate);
    }

}
