package com.garganttua.core.configuration.populator;

/**
 * Strategy controlling how configuration keys are matched against builder method names:
 * {@code DIRECT} (exact name only), {@code CAMEL_CASE}, {@code KEBAB_CASE}, or {@code SMART}
 * (tries all variants).
 */
public enum MethodMappingStrategy {

    DIRECT,
    CAMEL_CASE,
    KEBAB_CASE,
    SMART;

    /**
     * Parses a strategy name case-insensitively, defaulting to {@link #SMART} when blank.
     *
     * @param strategy the strategy name, may be {@code null} or empty
     * @return the matching strategy, or {@link #SMART} when {@code strategy} is null/empty
     * @throws IllegalArgumentException if {@code strategy} is non-blank but not a known constant
     */
    public static MethodMappingStrategy fromString(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            return SMART;
        }
        return valueOf(strategy.toUpperCase(java.util.Locale.ROOT));
    }
}
