package com.garganttua.core.configuration.populator;

public enum MethodMappingStrategy {

    DIRECT,
    CAMEL_CASE,
    KEBAB_CASE,
    SMART;

    public static MethodMappingStrategy fromString(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            return SMART;
        }
        return valueOf(strategy.toUpperCase());
    }
}
