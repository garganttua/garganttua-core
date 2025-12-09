package com.garganttua.core.condition;

import java.util.function.Function;
import java.util.function.Predicate;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Conditions {

    private Conditions() {

    }

    public static IConditionBuilder and(IConditionBuilder... conditions) {
        log.atTrace().log("Creating AND condition builder with {} conditions", conditions.length);
        return new AndConditionBuilder(conditions);
    }

    public static IConditionBuilder or(IConditionBuilder... conditions) {
        log.atTrace().log("Creating OR condition builder with {} conditions", conditions.length);
        return new OrConditionBuilder(conditions);
    }

    public static IConditionBuilder xor(IConditionBuilder... conditions) {
        log.atTrace().log("Creating XOR condition builder with {} conditions", conditions.length);
        return new XorConditionBuilder(conditions);
    }

    public static IConditionBuilder nand(IConditionBuilder... conditions) {
        log.atTrace().log("Creating NAND condition builder with {} conditions", conditions.length);
        return new NandConditionBuilder(conditions);
    }

    public static IConditionBuilder nor(IConditionBuilder... conditions) {
        log.atTrace().log("Creating NOR condition builder with {} conditions", conditions.length);
        return new NorConditionBuilder(conditions);
    }

    //
    // GENERICS
    //

    public static <T> IConditionBuilder notEquals(ISupplierBuilder<T, ISupplier<T>> supplier1,
            ISupplierBuilder<T, ISupplier<T>> supplier2) {
        log.atTrace().log("Creating NOT EQUALS condition builder with suppliers");
        return new NotEqualsConditionBuilder<T>(supplier1, supplier2);
    }

    public static <T> IConditionBuilder notEquals(T object1, T object2) {
        log.atTrace().log("Creating NOT EQUALS condition builder with fixed objects");
        return new NotEqualsConditionBuilder<T>(FixedSupplierBuilder.of(object1),
                FixedSupplierBuilder.of(object2));
    }

    public static <T> IConditionBuilder equals(ISupplierBuilder<T, ISupplier<T>> supplier1,
            ISupplierBuilder<T, ISupplier<T>> supplier2) {
        log.atTrace().log("Creating EQUALS condition builder with suppliers");
        return new EqualsConditionBuilder<T>(supplier1, supplier2);
    }

    public static <T> IConditionBuilder equals(T object1, T object2) {
        log.atTrace().log("Creating EQUALS condition builder with fixed objects");
        return new EqualsConditionBuilder<T>(FixedSupplierBuilder.of(object1),
                FixedSupplierBuilder.of(object2));
    }

    public static <T> IConditionBuilder isNotNull(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.atTrace().log("Creating NOT NULL condition builder with supplier");
        return new NotNullConditionBuilder<T>(supplier);
    }

    public static <T> IConditionBuilder isNotNull(T object) {
        log.atTrace().log("Creating NOT NULL condition builder with fixed object");
        return new NotNullConditionBuilder<>(FixedSupplierBuilder.of(object));
    }

    public static <T> IConditionBuilder isNull(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.atTrace().log("Creating NULL condition builder with supplier");
        return new NullConditionBuilder<T>(supplier);
    }

    public static <T> IConditionBuilder isNull(T object) {
        log.atTrace().log("Creating NULL condition builder with fixed object");
        return new NullConditionBuilder<>(FixedSupplierBuilder.of(object));
    }


    public static <T, R> CustomExtractedConditionBuilder<T, R> custom(
            ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.atTrace().log("Creating CUSTOM EXTRACTED condition builder");
        return new CustomExtractedConditionBuilder<>(builder, extractor, predicate);
    }

    public static <T> CustomConditionBuilder<T> custom(
            ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Predicate<T> predicate) {
        log.atTrace().log("Creating CUSTOM condition builder");
        return new CustomConditionBuilder<>(builder, predicate);
    }

}
