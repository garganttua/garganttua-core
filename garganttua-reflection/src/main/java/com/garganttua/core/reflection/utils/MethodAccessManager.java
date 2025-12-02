package com.garganttua.core.reflection.utils;

import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodAccessManager implements AutoCloseable {
	private final Method method;
	private final boolean originalAccessibility;

	@SuppressWarnings("deprecation")
	public MethodAccessManager(Method method) {
		log.atTrace().log("Creating MethodAccessManager for method={}", method);
		this.method = method;
		this.originalAccessibility = method.isAccessible();
		this.method.setAccessible(true);
		log.atDebug().log("Set method {} accessible, original accessibility={}", method.getName(), originalAccessibility);
	}

	@Override
	public void close() {
		log.atTrace().log("Closing MethodAccessManager, restoring accessibility={} for method={}", originalAccessibility, method.getName());
		this.method.setAccessible(originalAccessibility);
	}
}
