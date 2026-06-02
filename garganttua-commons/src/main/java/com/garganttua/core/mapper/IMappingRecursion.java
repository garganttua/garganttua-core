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

	/**
	 * Maps {@code source} to a new instance of {@code destinationClass}, reusing the
	 * call-scoped state of the enclosing mapping invocation (cycle-detection set, etc.).
	 *
	 * @param <destination>    the destination type
	 * @param source           the object to map from
	 * @param destinationClass the target type to map into
	 * @return the mapped destination instance
	 * @throws MapperException if the nested mapping fails
	 */
	<destination> destination map(Object source, IClass<destination> destinationClass) throws MapperException;
}
