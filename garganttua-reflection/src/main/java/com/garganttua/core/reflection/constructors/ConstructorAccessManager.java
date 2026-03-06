package com.garganttua.core.reflection.constructors;

import java.lang.reflect.Modifier;

import com.garganttua.core.reflection.IConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConstructorAccessManager implements AutoCloseable {
	private final IConstructor<?> constructor;
	private final boolean originalAccessibility;

	public ConstructorAccessManager(IConstructor<?> constructor) {
		this(constructor, false);
	}

	public ConstructorAccessManager(IConstructor<?> constructor, boolean force) {
		log.atTrace().log("Creating ConstructorAccessManager for constructor={}, force={}", constructor, force);
		this.constructor = constructor;
		this.originalAccessibility = Modifier.isPublic(constructor.getModifiers());
		if (force || !originalAccessibility) {
			this.constructor.setAccessible(true);
		}
		log.atDebug().log("Set constructor {} accessible, original accessibility={}, force={}", constructor.getName(), originalAccessibility, force);
	}

	@Override
	public void close() {
		log.atTrace().log("Closing ConstructorAccessManager, restoring accessibility={} for constructor={}", originalAccessibility, constructor.getName());
		this.constructor.setAccessible(originalAccessibility);
	}
}