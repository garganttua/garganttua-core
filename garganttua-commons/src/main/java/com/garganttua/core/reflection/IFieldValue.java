package com.garganttua.core.reflection;

import java.util.function.Function;

/**
 * Represents the result of a field access that can be either a single value
 * or multiple values (when accessed through a collection/array/map traversal).
 *
 * <p>
 * This interface extends {@link IInvocationResult} and adds field-specific
 * mapping capability.
 * </p>
 *
 * @param <F> the type of the field value(s)
 * @since 2.0.0-ALPHA01
 * @see IInvocationResult
 * @see IMethodReturn
 */
public interface IFieldValue<F> extends IInvocationResult<F> {

	/**
	 * Applies a transformation function to all values.
	 *
	 * @param <U>        the type of the transformed values
	 * @param mapper     the transformation function
	 * @param targetType the target IClass type
	 * @return a new IFieldValue with transformed values
	 */
	<U> IFieldValue<U> map(Function<? super F, ? extends U> mapper, IClass<U> targetType);

}
