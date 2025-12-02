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
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

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

    public static <T> IConditionBuilder notEquals(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1,
            IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2) {
        log.atTrace().log("Creating NOT EQUALS condition builder with suppliers");
        return new NotEqualsConditionBuilder<T>(supplier1, supplier2);
    }

    public static <T> IConditionBuilder notEquals(T object1, T object2) {
        log.atTrace().log("Creating NOT EQUALS condition builder with fixed objects");
        return new NotEqualsConditionBuilder<T>(FixedObjectSupplierBuilder.of(object1),
                FixedObjectSupplierBuilder.of(object2));
    }

    public static <T> IConditionBuilder equals(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1,
            IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2) {
        log.atTrace().log("Creating EQUALS condition builder with suppliers");
        return new EqualsConditionBuilder<T>(supplier1, supplier2);
    }

    public static <T> IConditionBuilder equals(T object1, T object2) {
        log.atTrace().log("Creating EQUALS condition builder with fixed objects");
        return new EqualsConditionBuilder<T>(FixedObjectSupplierBuilder.of(object1),
                FixedObjectSupplierBuilder.of(object2));
    }

    public static <T> IConditionBuilder isNotNull(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        log.atTrace().log("Creating NOT NULL condition builder with supplier");
        return new NotNullConditionBuilder<T>(supplier);
    }

    public static <T> IConditionBuilder isNotNull(T object) {
        log.atTrace().log("Creating NOT NULL condition builder with fixed object");
        return new NotNullConditionBuilder<>(FixedObjectSupplierBuilder.of(object));
    }

    public static <T> IConditionBuilder isNull(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        log.atTrace().log("Creating NULL condition builder with supplier");
        return new NullConditionBuilder<T>(supplier);
    }

    public static <T> IConditionBuilder isNull(T object) {
        log.atTrace().log("Creating NULL condition builder with fixed object");
        return new NullConditionBuilder<>(FixedObjectSupplierBuilder.of(object));
    }


    public static <T, R> CustomExtractedConditionBuilder<T, R> custom(
            IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.atTrace().log("Creating CUSTOM EXTRACTED condition builder");
        return new CustomExtractedConditionBuilder<>(builder, extractor, predicate);
    }

    public static <T> CustomConditionBuilder<T> custom(
            IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Predicate<T> predicate) {
        log.atTrace().log("Creating CUSTOM condition builder");
        return new CustomConditionBuilder<>(builder, predicate);
    }

}
