package com.garganttua.reflection.utils;

import java.lang.reflect.Constructor;

public class GGConstructorAccessManager implements AutoCloseable {
	private final Constructor<?> constructor;
	private final boolean originalAccessibility;

	@SuppressWarnings("deprecation")
	public GGConstructorAccessManager(Constructor<?> constructor) {
		this.constructor = constructor;
		this.originalAccessibility = constructor.isAccessible();
		this.constructor.setAccessible(true);
	}

	@Override
	public void close() {
		this.constructor.setAccessible(originalAccessibility);
	}
}