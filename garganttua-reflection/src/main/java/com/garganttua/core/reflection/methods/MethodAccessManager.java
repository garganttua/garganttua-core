package com.garganttua.core.reflection.methods;

import java.lang.reflect.Modifier;

import com.garganttua.core.reflection.IMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodAccessManager implements AutoCloseable {
	private final IMethod method;
	private final boolean originalAccessibility;

	public MethodAccessManager(IMethod method) {
		this(method, false);
	}

	public MethodAccessManager(IMethod method, boolean force) {
		log.atTrace().log("Creating MethodAccessManager for method={}, force={}", method, force);
		this.method = method;
		this.originalAccessibility = Modifier.isPublic(method.getModifiers())
				&& Modifier.isPublic(method.getDeclaringClass().getModifiers());
		this.method.setAccessible(true);
		log.atDebug().log("Set method {} accessible, original accessibility={}, force={}", method.getName(), originalAccessibility, force);
	}

	@Override
	public void close() {
		log.atTrace().log("Closing MethodAccessManager, restoring accessibility={} for method={}", originalAccessibility, method.getName());
		this.method.setAccessible(originalAccessibility);
	}
}