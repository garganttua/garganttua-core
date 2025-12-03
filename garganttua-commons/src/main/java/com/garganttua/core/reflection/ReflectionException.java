package com.garganttua.core.reflection;

import com.garganttua.core.CoreException;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when errors occur during reflection operations.
 *
 * <p>
 * {@code ReflectionException} is the primary exception type for all reflection-related
 * errors in the Garganttua framework. It is thrown when reflection operations fail due
 * to access restrictions, invalid addresses, type mismatches, invocation errors, or
 * other reflection-related problems.
 * </p>
 *
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Field or method not found by name or address</li>
 *   <li>Illegal access to private members</li>
 *   <li>Type mismatch during field assignment or method invocation</li>
 *   <li>Invalid object addresses or field paths</li>
 *   <li>Loop detection in object address navigation</li>
 *   <li>Invocation target exceptions from method calls</li>
 *   <li>Instantiation failures during object construction</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IObjectQuery query = ...;
 * try {
 *     Object value = query.getValue(object, "user.profile.email");
 * } catch (ReflectionException e) {
 *     // Handle reflection failure
 *     logger.error("Failed to access field: " + e.getMessage(), e);
 *     // Fallback or error recovery
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see CoreException
 * @see IObjectQuery
 */
@Slf4j
public class ReflectionException extends CoreException {

	/**
	 * Constructs a new reflection exception with the specified message.
	 *
	 * @param string the detailed error message describing what went wrong
	 */
	public ReflectionException(String string) {
		super(CoreException.REFLECTION_ERROR, string);
		log.atTrace().log("Exiting ReflectionException constructor");
	}

	/**
	 * Constructs a new reflection exception with the specified message and cause.
	 *
	 * @param string the detailed error message describing what went wrong
	 * @param e the underlying exception that caused this error
	 */
	public ReflectionException(String string, Throwable e) {
		super(CoreException.REFLECTION_ERROR, string, e);
		log.atTrace().log("Exiting ReflectionException constructor");
	}

	/**
	 * Constructs a new reflection exception wrapping another exception.
	 *
	 * @param e the exception to wrap
	 */
	public ReflectionException(Throwable e) {
		super(e);
		log.atTrace().log("Exiting ReflectionException constructor");
	}

	private static final long serialVersionUID = 2732095843634378815L;

}
