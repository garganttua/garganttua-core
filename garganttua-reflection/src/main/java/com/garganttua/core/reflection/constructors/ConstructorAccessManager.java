package com.garganttua.core.reflection.constructors;

import java.lang.reflect.Modifier;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IConstructor;

/**
 * {@link AutoCloseable} guard that makes an {@link IConstructor} accessible for the duration of a
 * try-with-resources block and restores its original accessibility on {@link #close()}.
 */
public class ConstructorAccessManager implements AutoCloseable {
    private static final Logger log = Logger.getLogger(ConstructorAccessManager.class);

	private final IConstructor<?> constructor;
	private final boolean originalAccessibility;

	/**
	 * Equivalent to {@link #ConstructorAccessManager(IConstructor, boolean)} with {@code force = false}.
	 *
	 * @param constructor the constructor to make accessible
	 */
	public ConstructorAccessManager(IConstructor<?> constructor) {
		this(constructor, false);
	}

	/**
	 * Makes the given constructor accessible, recording its original accessibility for later restoration.
	 *
	 * @param constructor the constructor to make accessible
	 * @param force       whether access was forced (recorded for diagnostics)
	 */
	public ConstructorAccessManager(IConstructor<?> constructor, boolean force) {
		log.trace("Creating ConstructorAccessManager for constructor={}, force={}", constructor, force);
		this.constructor = constructor;
		this.originalAccessibility = Modifier.isPublic(constructor.getModifiers())
				&& Modifier.isPublic(constructor.getDeclaringClass().getModifiers());
		this.constructor.setAccessible(true);
		log.debug("Set constructor {} accessible, original accessibility={}, force={}", constructor.getName(), originalAccessibility, force);
	}

	/** Restores the constructor's original accessibility. */
	@Override
	public void close() {
		log.trace("Closing ConstructorAccessManager, restoring accessibility={} for constructor={}", originalAccessibility, constructor.getName());
		this.constructor.setAccessible(originalAccessibility);
	}
}