package com.garganttua.core.reflection.methods;

import java.lang.reflect.Modifier;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IMethod;

/**
 * Scoped accessibility toggle for an {@link IMethod}.
 *
 * <p>On construction the method is made accessible; on {@link #close()} its original
 * accessibility is restored, making it suitable for use in a try-with-resources block.
 */
public class MethodAccessManager implements AutoCloseable {
    private static final Logger log = Logger.getLogger(MethodAccessManager.class);

	private final IMethod method;
	private final boolean originalAccessibility;

	/**
	 * Makes {@code method} accessible without forcing access on inaccessible members.
	 *
	 * @param method the method whose accessibility is managed
	 */
	public MethodAccessManager(IMethod method) {
		this(method, false);
	}

	/**
	 * Makes {@code method} accessible.
	 *
	 * @param method the method whose accessibility is managed
	 * @param force  whether to force access even for non-public members
	 */
	public MethodAccessManager(IMethod method, boolean force) {
		log.trace("Creating MethodAccessManager for method={}, force={}", method, force);
		this.method = method;
		this.originalAccessibility = Modifier.isPublic(method.getModifiers())
				&& Modifier.isPublic(method.getDeclaringClass().getModifiers());
		this.method.setAccessible(true);
		log.debug("Set method {} accessible, original accessibility={}, force={}", method.getName(), originalAccessibility, force);
	}

	/** Restores the method's original accessibility. */
	@Override
	public void close() {
		log.trace("Closing MethodAccessManager, restoring accessibility={} for method={}", originalAccessibility, method.getName());
		this.method.setAccessible(originalAccessibility);
	}
}