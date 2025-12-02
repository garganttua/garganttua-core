package com.garganttua.core.reflection.utils;

import java.lang.reflect.Field;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldAccessManager implements AutoCloseable {
	private final Field field;
	private final boolean originalAccessibility;

	@SuppressWarnings("deprecation")
	public FieldAccessManager(Field field) {
		log.atTrace().log("Creating FieldAccessManager for field={}", field);
		this.field = field;
		this.originalAccessibility = field.isAccessible();
		this.field.setAccessible(true);
		log.atDebug().log("Set field {} accessible, original accessibility={}", field.getName(), originalAccessibility);
	}

	@Override
	public void close() {
		log.atTrace().log("Closing FieldAccessManager, restoring accessibility={} for field={}", originalAccessibility, field.getName());
		this.field.setAccessible(originalAccessibility);
	}
}
