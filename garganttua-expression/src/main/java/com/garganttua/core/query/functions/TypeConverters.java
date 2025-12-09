package com.garganttua.core.query.functions;

import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

/**
 * Type converter functions for query language.
 * These functions convert primitive values to ISupplier instances.
 */
public class TypeConverters {

    /**
     * Converts a String value to an ISupplier<String>
     *
     * @param value the string value
     * @return an ISupplier that supplies the string value
     */
    public static ISupplier<String> string(String value) {
        return new FixedSupplierBuilder<String>(value).build();
    }

    /**
     * Converts an Integer value to an ISupplier<Integer>
     *
     * @param value the integer value
     * @return an ISupplier that supplies the integer value
     */
    public static ISupplier<Integer> integer(Integer value) {
        return new FixedSupplierBuilder<Integer>(value).build();
    }

    /**
     * Converts a Long value to an ISupplier<Long>
     *
     * @param value the long value
     * @return an ISupplier that supplies the long value
     */
    public static ISupplier<Long> longValue(Long value) {
        return new FixedSupplierBuilder<Long>(value).build();
    }

    /**
     * Converts a Double value to an ISupplier<Double>
     *
     * @param value the double value
     * @return an ISupplier that supplies the double value
     */
    public static ISupplier<Double> doubleValue(Double value) {
        return new FixedSupplierBuilder<Double>(value).build();
    }

    /**
     * Converts a Float value to an ISupplier<Float>
     *
     * @param value the float value
     * @return an ISupplier that supplies the float value
     */
    public static ISupplier<Float> floatValue(Float value) {
        return new FixedSupplierBuilder<Float>(value).build();
    }

    /**
     * Converts a Boolean value to an ISupplier<Boolean>
     *
     * @param value the boolean value
     * @return an ISupplier that supplies the boolean value
     */
    public static ISupplier<Boolean> booleanValue(Boolean value) {
        return new FixedSupplierBuilder<Boolean>(value).build();
    }

}
