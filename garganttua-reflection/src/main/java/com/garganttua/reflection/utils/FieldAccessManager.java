package com.garganttua.reflection.utils;

import java.lang.reflect.Field;

public class FieldAccessManager implements AutoCloseable {
	private final Field field;
	private final boolean originalAccessibility;

	@SuppressWarnings("deprecation")
	public FieldAccessManager(Field field) {
		this.field = field;
		this.originalAccessibility = field.isAccessible();
		this.field.setAccessible(true);
	}

	@Override
	public void close() {
		this.field.setAccessible(originalAccessibility);
	}
}
