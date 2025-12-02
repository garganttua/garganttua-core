package com.garganttua.core.nativve.image.config.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.nativve.annotations.Native;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectConfigEntryBuilder extends AbstractAutomaticBuilder<IReflectionConfigurationEntryBuilder, IReflectionConfigurationEntry> implements IReflectionConfigurationEntryBuilder {

	private final IReflectionConfigurationEntry entry;
	private Class<?> type;

	public ReflectConfigEntryBuilder(Class<?> type) {
		log.atTrace().log("Creating ReflectConfigEntryBuilder for type: {}", type.getName());
		this.type = Objects.requireNonNull(type, "Type cannot be null");
		this.entry = new ReflectConfigEntry(type.getName());
		this.entry.setFields(new ArrayList<>());
		this.entry.setMethods(new ArrayList<>());
		log.atDebug().log("Initialized ReflectConfigEntryBuilder for: {}", type.getName());
	}

	public ReflectConfigEntryBuilder(IReflectionConfigurationEntry entry) throws DslException {
		log.atTrace().log("Creating ReflectConfigEntryBuilder from existing entry: {}", entry.getName());
		this.entry = entry;
		try {
			this.type = entry.getEntryClass();
			log.atDebug().log("Loaded entry class: {}", this.type.getName());
		} catch (ClassNotFoundException e) {
			log.atError().log("Failed to load entry class: {}", entry.getName());
			throw new DslException(e);
		}
	}

	public static IReflectionConfigurationEntryBuilder builder(Class<?> clazz) {
		return new ReflectConfigEntryBuilder(clazz);
	}

	public static IReflectionConfigurationEntryBuilder builder(ReflectConfigEntry entry) throws DslException {
		return new ReflectConfigEntryBuilder(entry);
	}

	@Override
	public IReflectionConfigurationEntryBuilder queryAllDeclaredConstructors(boolean value) {
		entry.setQueryAllDeclaredConstructors(value);
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder queryAllPublicConstructors(boolean value) {
		entry.setQueryAllPublicConstructors(value);
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder queryAllDeclaredMethods(boolean value) {
		entry.setQueryAllDeclaredMethods(value);
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder queryAllPublicMethods(boolean value) {
		entry.setQueryAllPublicMethods(value);
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder allDeclaredClasses(boolean value) {
		entry.setAllDeclaredClasses(value);
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder allPublicClasses(boolean value) {
		entry.setAllPublicClasses(value);
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder field(String fieldName) {
		log.atTrace().log("Adding field: {}", fieldName);
		if (entry.getFields().stream().noneMatch(field -> field.getName().equals(fieldName))) {
			ReflectConfigEntry.Field field = new ReflectConfigEntry.Field();
			field.setName(fieldName);
			entry.getFields().add(field);
			log.atDebug().log("Added field to reflection config: {}", fieldName);
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder field(Field field) {
		return field(field.getName());
	}

	@Override
	public IReflectionConfigurationEntryBuilder method(String methodName, Class<?>... parameterTypes) {
		log.atTrace().log("Adding method: {} with {} parameters", methodName, parameterTypes.length);
		List<String> paramNames = List.of(parameterTypes).stream().map(Class::getName).collect(Collectors.toList());

		if (entry.getMethods().stream()
				.noneMatch(m -> m.getName().equals(methodName) && m.getParameterTypes().equals(paramNames))) {
			ReflectConfigEntry.Method method = new ReflectConfigEntry.Method();
			method.setName(methodName);
			method.setParameterTypes(paramNames);
			entry.getMethods().add(method);
			log.atDebug().log("Added method to reflection config: {}", methodName);
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder method(Method method) {
		return method(method.getName(), method.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder constructor(String constructorName, Class<?>... parameterTypes) {
		log.atTrace().log("Adding constructor with {} parameters", parameterTypes.length);
		return method("<init>", parameterTypes);
	}

	@Override
	public IReflectionConfigurationEntryBuilder constructor(Constructor<?> ctor) {
		return constructor("<init>", ctor.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder fieldsAnnotatedWith(Class<? extends Annotation> annotation) {
		log.atTrace().log("Adding fields annotated with: {}", annotation.getName());
		try {
			for (Field field : this.entry.getEntryClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(annotation)) {
					field(field.getName());
				}
			}
			log.atDebug().log("Added all fields annotated with: {}", annotation.getName());
		} catch (SecurityException | ClassNotFoundException e) {
			log.atWarn().log("Error processing fields annotated with {}: {}", annotation.getName(), e.getMessage());
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder methodsAnnotatedWith(Class<? extends Annotation> annotation) {
		log.atTrace().log("Adding methods annotated with: {}", annotation.getName());
		try {
			for (Method method : this.entry.getEntryClass().getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotation)) {
					method(method);
				}
			}
			log.atDebug().log("Added all methods annotated with: {}", annotation.getName());
		} catch (SecurityException | ClassNotFoundException e) {
			log.atWarn().log("Error processing methods annotated with {}: {}", annotation.getName(), e.getMessage());
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeField(String fieldName) {
		entry.setFields(entry.getFields().stream().filter(field -> !field.getName().equals(fieldName))
				.collect(Collectors.toList()));
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeField(Field field) {
		return removeField(field.getName());
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeMethod(String methodName, Class<?>... parameterTypes) {
		entry.setMethods(entry.getMethods().stream().filter(method -> !method.getName().equals(methodName)
				|| !method.getParameterTypes().equals(List.of(parameterTypes).stream().map(Class::getName).toList()))
				.collect(Collectors.toList()));
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeMethod(Method method) {
		return removeMethod(method.getName(), method.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeConstructor(String constructorName, Class<?>... parameterTypes) {
		return removeMethod("<init>", parameterTypes);
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeConstructor(Constructor<?> ctor) {
		return removeConstructor("<init>", ctor.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
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
	public IReflectionConfigurationEntryBuilder removeMethodAnnotatedWith(Class<? extends Annotation> annotation) {
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
	public IReflectionConfigurationEntryBuilder allDeclaredFields(boolean value) {
		entry.setAllDeclaredFields(value);
		return this;
	}

	@Override
	protected IReflectionConfigurationEntry doBuild() throws DslException {
		log.atTrace().log("Building reflection configuration entry for: {}", entry.getName());
		log.atInfo().log("Reflection configuration entry built for: {}", entry.getName());
		return entry;
	}

	@Override
	protected void doAutoDetection() throws DslException {
		log.atTrace().log("Starting auto-detection for type: {}", this.type.getName());
		Native n = this.type.getAnnotation(Native.class);
		if (n != null && n.allDeclaredClasses()) {
			log.atDebug().log("Enabling allDeclaredClasses for: {}", this.type.getName());
			this.entry.setAllDeclaredClasses(true);
		}
		if (n != null && n.allDeclaredFields()) {
			log.atDebug().log("Enabling allDeclaredFields for: {}", this.type.getName());
			this.entry.setAllDeclaredFields(true);
		}
		if (n != null && n.allPublicClasses()) {
			log.atDebug().log("Enabling allPublicClasses for: {}", this.type.getName());
			this.entry.setAllPublicClasses(true);
		}
		if (n != null && n.queryAllDeclaredConstructors()) {
			log.atDebug().log("Enabling queryAllDeclaredConstructors for: {}", this.type.getName());
			this.entry.setQueryAllDeclaredConstructors(true);
		}
		if (n != null && n.queryAllDeclaredMethods()) {
			log.atDebug().log("Enabling queryAllDeclaredMethods for: {}", this.type.getName());
			this.entry.setQueryAllDeclaredMethods(true);
		}
		if (n != null && n.queryAllPublicConstructors()) {
			log.atDebug().log("Enabling queryAllPublicConstructors for: {}", this.type.getName());
			this.entry.setQueryAllPublicConstructors(true);
		}
		if (n != null && n.queryAllPublicMethods()) {
			log.atDebug().log("Enabling queryAllPublicMethods for: {}", this.type.getName());
			this.entry.setQueryAllPublicMethods(false);
		}

		log.atDebug().log("Detecting @Native annotated fields, constructors, and methods for: {}", this.type.getName());
		Arrays.stream(this.type.getDeclaredFields()).filter(f -> f.getAnnotation(Native.class)!=null).forEach(this::field);
		Arrays.stream(this.type.getDeclaredConstructors()).filter(c -> c.getAnnotation(Native.class)!=null).forEach(this::constructor);
		Arrays.stream(this.type.getDeclaredMethods()).filter(m -> m.getAnnotation(Native.class)!=null).forEach(this::method);
		log.atTrace().log("Completed auto-detection for type: {}", this.type.getName());
	}

}