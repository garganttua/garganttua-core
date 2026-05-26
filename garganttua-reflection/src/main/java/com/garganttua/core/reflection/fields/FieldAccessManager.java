package com.garganttua.core.reflection.fields;

import java.lang.reflect.Modifier;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IField;

public class FieldAccessManager implements AutoCloseable {
    private static final IDiagnostic log = Diagnostics.of(FieldAccessManager.class);

	private final IField field;
	private final boolean originalAccessibility;

	public FieldAccessManager(IField field) {
		this(field, false);
	}

	public FieldAccessManager(IField field, boolean force) {
		log.trace("Creating FieldAccessManager for field={}, force={}", field, force);
		this.field = field;
		this.originalAccessibility = Modifier.isPublic(field.getModifiers())
				&& Modifier.isPublic(field.getDeclaringClass().getModifiers());
		this.field.setAccessible(true);
		log.debug("Set field {} accessible, original accessibility={}, force={}", field.getName(), originalAccessibility, force);
	}

	@Override
	public void close() {
		log.trace("Closing FieldAccessManager, restoring accessibility={} for field={}", originalAccessibility, field.getName());
		this.field.setAccessible(originalAccessibility);
	}
}