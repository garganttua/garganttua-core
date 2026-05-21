package com.garganttua.core.mapper;

import com.garganttua.core.reflection.IClass;

/**
 * Callback handed to a {@link IMappingRuleExecutor} so it can recurse into the
 * current mapper while preserving the call-scoped state (cycle-detection set,
 * etc.) without relying on a {@code ThreadLocal}.
 *
 * <p>
 * Implementations are typically lambdas created by the {@link IMapper} for each
 * top-level mapping invocation, closing over the scoped state.
 *
 * <p>
 * Not a {@code @FunctionalInterface}: its single method is generic, which
 * forbids lambda construction. Use an anonymous inner class instead.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IMappingRecursion {

	<destination> destination map(Object source, IClass<destination> destinationClass) throws MapperException;
}
