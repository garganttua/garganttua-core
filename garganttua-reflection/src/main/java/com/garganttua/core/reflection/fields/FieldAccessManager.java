package com.garganttua.core.reflection.fields;

import java.lang.reflect.Modifier;

import com.garganttua.core.reflection.IField;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldAccessManager implements AutoCloseable {
	private final IField field;
	private final boolean originalAccessibility;

	public FieldAccessManager(IField field) {
		this(field, false);
	}

	public FieldAccessManager(IField field, boolean force) {
		log.atTrace().log("Creating FieldAccessManager for field={}, force={}", field, force);
		this.field = field;
		this.originalAccessibility = Modifier.isPublic(field.getModifiers())
				&& Modifier.isPublic(field.getDeclaringClass().getModifiers());
		this.field.setAccessible(true);
		log.atDebug().log("Set field {} accessible, original accessibility={}, force={}", field.getName(), originalAccessibility, force);
	}

	@Override
	public void close() {
		log.atTrace().log("Closing FieldAccessManager, restoring accessibility={} for field={}", originalAccessibility, field.getName());
		this.field.setAccessible(originalAccessibility);
	}
}