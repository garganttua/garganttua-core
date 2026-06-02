package com.garganttua.core.injection;

/**
 * Immutable two-element tuple holding a pair of related values.
 *
 * @param <T1> the type of the first value
 * @param <T2> the type of the second value
 * @param value1 the first value
 * @param value2 the second value
 * @since 2.0.0-ALPHA01
 */
public record Pair<T1, T2> (T1 value1, T2 value2) {


}
