package com.garganttua.core.reflection.methods;

import java.lang.reflect.Modifier;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IMethod;

public class MethodAccessManager implements AutoCloseable {
    private static final IDiagnostic log = Diagnostics.of(MethodAccessManager.class);

	private final IMethod method;
	private final boolean originalAccessibility;

	public MethodAccessManager(IMethod method) {
		this(method, false);
	}

	public MethodAccessManager(IMethod method, boolean force) {
		log.trace("Creating MethodAccessManager for method={}, force={}", method, force);
		this.method = method;
		this.originalAccessibility = Modifier.isPublic(method.getModifiers())
				&& Modifier.isPublic(method.getDeclaringClass().getModifiers());
		this.method.setAccessible(true);
		log.debug("Set method {} accessible, original accessibility={}, force={}", method.getName(), originalAccessibility, force);
	}

	@Override
	public void close() {
		log.trace("Closing MethodAccessManager, restoring accessibility={} for method={}", originalAccessibility, method.getName());
		this.method.setAccessible(originalAccessibility);
	}
}