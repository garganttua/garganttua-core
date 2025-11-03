package com.garganttua.core.reflection.utils;

import java.lang.reflect.Method;

public class MethodAccessManager implements AutoCloseable {
	private final Method method;
	private final boolean originalAccessibility;

	@SuppressWarnings("deprecation")
	public MethodAccessManager(Method method) {
		this.method = method;
		this.originalAccessibility = method.isAccessible();
		this.method.setAccessible(true);
	}

	@Override
	public void close() {
		this.method.setAccessible(originalAccessibility);
	}
}
