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

public class Conditions {

    private Conditions() {

    }

    public static IConditionBuilder and(IConditionBuilder... conditions) {
        return new AndConditionBuilder(conditions);
    }

    public static IConditionBuilder or(IConditionBuilder... conditions) {
        return new OrConditionBuilder(conditions);
    }

    public static IConditionBuilder xor(IConditionBuilder... conditions) {
        return new XorConditionBuilder(conditions);
    }

    public static IConditionBuilder nand(IConditionBuilder... conditions) {
        return new NandConditionBuilder(conditions);
    }

    public static IConditionBuilder nor(IConditionBuilder... conditions) {
        return new NorConditionBuilder(conditions);
    }

    //
    // GENERICS
    //

    public static <T> IConditionBuilder notEquals(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1,
            IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2) {
        return new NotEqualsConditionBuilder<T>(supplier1, supplier2);
    }

    public static <T> IConditionBuilder notEquals(T object1, T object2) {
        return new NotEqualsConditionBuilder<T>(FixedObjectSupplierBuilder.of(object1),
                FixedObjectSupplierBuilder.of(object2));
    }

    public static <T> IConditionBuilder equals(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1,
            IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2) {
        return new EqualsConditionBuilder<T>(supplier1, supplier2);
    }

    public static <T> IConditionBuilder equals(T object1, T object2) {
        return new EqualsConditionBuilder<T>(FixedObjectSupplierBuilder.of(object1),
                FixedObjectSupplierBuilder.of(object2));
    }

    public static <T> IConditionBuilder isNotNull(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        return new NotNullConditionBuilder<T>(supplier);
    }

    public static <T> IConditionBuilder isNotNull(T object) {
        return new NotNullConditionBuilder<>(FixedObjectSupplierBuilder.of(object));
    }

    public static <T> IConditionBuilder isNull(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        return new NullConditionBuilder<T>(supplier);
    }

    public static <T> IConditionBuilder isNull(T object) {
        return new NullConditionBuilder<>(FixedObjectSupplierBuilder.of(object));
    }


    public static <T, R> CustomExtractedConditionBuilder<T, R> custom(
            IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        return new CustomExtractedConditionBuilder<>(builder, extractor, predicate);
    }

    public static <T> CustomConditionBuilder<T> custom(
            IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Predicate<T> predicate) {
        return new CustomConditionBuilder<>(builder, predicate);
    }

}
