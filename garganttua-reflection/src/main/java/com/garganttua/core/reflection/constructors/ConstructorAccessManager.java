package com.garganttua.core.reflection.constructors;

import java.lang.reflect.Modifier;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IConstructor;

public class ConstructorAccessManager implements AutoCloseable {
    private static final Logger log = Logger.getLogger(ConstructorAccessManager.class);

	private final IConstructor<?> constructor;
	private final boolean originalAccessibility;

	public ConstructorAccessManager(IConstructor<?> constructor) {
		this(constructor, false);
	}

	public ConstructorAccessManager(IConstructor<?> constructor, boolean force) {
		log.trace("Creating ConstructorAccessManager for constructor={}, force={}", constructor, force);
		this.constructor = constructor;
		this.originalAccessibility = Modifier.isPublic(constructor.getModifiers())
				&& Modifier.isPublic(constructor.getDeclaringClass().getModifiers());
		this.constructor.setAccessible(true);
		log.debug("Set constructor {} accessible, original accessibility={}, force={}", constructor.getName(), originalAccessibility, force);
	}

	@Override
	public void close() {
		log.trace("Closing ConstructorAccessManager, restoring accessibility={} for constructor={}", originalAccessibility, constructor.getName());
		this.constructor.setAccessible(originalAccessibility);
	}
}