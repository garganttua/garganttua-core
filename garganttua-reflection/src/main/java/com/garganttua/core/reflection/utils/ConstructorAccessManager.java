package com.garganttua.core.reflection.utils;

import java.lang.reflect.Constructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConstructorAccessManager implements AutoCloseable {
	private final Constructor<?> constructor;
	private final boolean originalAccessibility;

	@SuppressWarnings("deprecation")
	public ConstructorAccessManager(Constructor<?> constructor) {
		log.atTrace().log("Creating ConstructorAccessManager for constructor={}", constructor);
		this.constructor = constructor;
		this.originalAccessibility = constructor.isAccessible();
		this.constructor.setAccessible(true);
		log.atDebug().log("Set constructor {} accessible, original accessibility={}", constructor.getName(), originalAccessibility);
	}

	@Override
	public void close() {
		log.atTrace().log("Closing ConstructorAccessManager, restoring accessibility={} for constructor={}", originalAccessibility, constructor.getName());
		this.constructor.setAccessible(originalAccessibility);
	}
}