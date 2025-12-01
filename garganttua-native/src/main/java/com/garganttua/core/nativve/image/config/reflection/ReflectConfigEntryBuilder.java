package com.garganttua.core.nativve.image.config.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectConfigEntryBuilder implements IReflectConfigEntryBuilder {

	private final ReflectConfigEntry entry;

	private ReflectConfigEntryBuilder(Class<?> clazz) {
		this.entry = new ReflectConfigEntry(clazz.getName());
		this.entry.setFields(new ArrayList<>());
		this.entry.setMethods(new ArrayList<>());
	}

	private ReflectConfigEntryBuilder(ReflectConfigEntry entry) {
		this.entry = entry;
	}

	public static IReflectConfigEntryBuilder builder(Class<?> clazz) {
		return new ReflectConfigEntryBuilder(clazz);
	}

	public static IReflectConfigEntryBuilder builder(ReflectConfigEntry entry) {
		return new ReflectConfigEntryBuilder(entry);
	}

	@Override
	public IReflectConfigEntryBuilder queryAllDeclaredConstructors(boolean value) {
		entry.setQueryAllDeclaredConstructors(value);
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder queryAllPublicConstructors(boolean value) {
		entry.setQueryAllPublicConstructors(value);
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder queryAllDeclaredMethods(boolean value) {
		entry.setQueryAllDeclaredMethods(value);
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder queryAllPublicMethods(boolean value) {
		entry.setQueryAllPublicMethods(value);
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder allDeclaredClasses(boolean value) {
		entry.setAllDeclaredClasses(value);
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder allPublicClasses(boolean value) {
		entry.setAllPublicClasses(value);
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder field(String fieldName) {
		if (entry.getFields().stream().noneMatch(field -> field.getName().equals(fieldName))) {
			ReflectConfigEntry.Field field = new ReflectConfigEntry.Field();
			field.setName(fieldName);
			entry.getFields().add(field);
		}
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder field(Field field) {
		return field(field.getName());
	}

	@Override
	public IReflectConfigEntryBuilder method(String methodName, Class<?>... parameterTypes) {
		List<String> paramNames = List.of(parameterTypes).stream().map(Class::getName).collect(Collectors.toList());

		if (entry.getMethods().stream()
				.noneMatch(m -> m.getName().equals(methodName) && m.getParameterTypes().equals(paramNames))) {
			ReflectConfigEntry.Method method = new ReflectConfigEntry.Method();
			method.setName(methodName);
			method.setParameterTypes(paramNames);
			entry.getMethods().add(method);
		}
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder method(Method method) {
		return method(method.getName(), method.getParameterTypes());
	}

	@Override
	public IReflectConfigEntryBuilder constructor(String constructorName, Class<?>... parameterTypes) {
		return method("<init>", parameterTypes);
	}

	@Override
	public IReflectConfigEntryBuilder constructor(Constructor<?> ctor) {
		return constructor("<init>", ctor.getParameterTypes());
	}

	@Override
	public IReflectConfigEntryBuilder fieldsAnnotatedWith(Class<? extends Annotation> annotation) {
		try {
			for (Field field : this.entry.getEntryClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(annotation)) {
					field(field.getName());
				}
			}
		} catch (SecurityException | ClassNotFoundException e) {
			log.atWarn().log("Error", e);
		}
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder methodsAnnotatedWith(Class<? extends Annotation> annotation) {
		try {
			for (Method method : this.entry.getEntryClass().getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotation)) {
					method(method);
				}
			}
		} catch (SecurityException | ClassNotFoundException e) {
			log.atWarn().log("Error", e);
		}
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder removeField(String fieldName) {
		entry.setFields(entry.getFields().stream().filter(field -> !field.getName().equals(fieldName))
				.collect(Collectors.toList()));
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder removeField(Field field) {
		return removeField(field.getName());
	}

	@Override
	public IReflectConfigEntryBuilder removeMethod(String methodName, Class<?>... parameterTypes) {
		entry.setMethods(entry.getMethods().stream().filter(method -> !method.getName().equals(methodName)
				|| !method.getParameterTypes().equals(List.of(parameterTypes).stream().map(Class::getName).toList()))
				.collect(Collectors.toList()));
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder removeMethod(Method method) {
		return removeMethod(method.getName(), method.getParameterTypes());
	}

	@Override
	public IReflectConfigEntryBuilder removeConstructor(String constructorName, Class<?>... parameterTypes) {
		return removeMethod("<init>", parameterTypes);
	}

	@Override
	public IReflectConfigEntryBuilder removeConstructor(Constructor<?> ctor) {
		return removeConstructor("<init>", ctor.getParameterTypes());
	}

	@Override
	public IReflectConfigEntryBuilder removeFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
		entry.setFields(entry.getFields().stream().filter(field -> {
			try {
				Field f = this.entry.getEntryClass().getDeclaredField(field.getName());
				return !f.isAnnotationPresent(annotation);
			} catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
				return true;
			}
		}).collect(Collectors.toList()));
		return this;
	}

	@Override
	public IReflectConfigEntryBuilder removeMethodAnnotatedWith(Class<? extends Annotation> annotation) {
		entry.setMethods(entry.getMethods().stream().filter(method -> {
			try {
				Method m = this.entry.getEntryClass().getDeclaredMethod(method.getName());
				return !m.isAnnotationPresent(annotation);
			} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
				return true;
			}
		}).collect(Collectors.toList()));
		return this;
	}

	@Override
	public ReflectConfigEntry build() {
		return entry;
	}

	@Override
	public IReflectConfigEntryBuilder allDeclaredFields(boolean value) {
		entry.setAllDeclaredFields(value);
		return this;
	}
}