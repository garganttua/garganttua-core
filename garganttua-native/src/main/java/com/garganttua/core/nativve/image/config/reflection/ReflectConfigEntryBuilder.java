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
		this.type = Objects.requireNonNull(type, "Type cannot be null");
		this.entry = new ReflectConfigEntry(type.getName());
		this.entry.setFields(new ArrayList<>());
		this.entry.setMethods(new ArrayList<>());
	}

	public ReflectConfigEntryBuilder(IReflectionConfigurationEntry entry) throws DslException {
		this.entry = entry;
		try {
			this.type = entry.getEntryClass();
		} catch (ClassNotFoundException e) {
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
		if (entry.getFields().stream().noneMatch(field -> field.getName().equals(fieldName))) {
			ReflectConfigEntry.Field field = new ReflectConfigEntry.Field();
			field.setName(fieldName);
			entry.getFields().add(field);
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder field(Field field) {
		return field(field.getName());
	}

	@Override
	public IReflectionConfigurationEntryBuilder method(String methodName, Class<?>... parameterTypes) {
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
	public IReflectionConfigurationEntryBuilder method(Method method) {
		return method(method.getName(), method.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder constructor(String constructorName, Class<?>... parameterTypes) {
		return method("<init>", parameterTypes);
	}

	@Override
	public IReflectionConfigurationEntryBuilder constructor(Constructor<?> ctor) {
		return constructor("<init>", ctor.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder fieldsAnnotatedWith(Class<? extends Annotation> annotation) {
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
	public IReflectionConfigurationEntryBuilder methodsAnnotatedWith(Class<? extends Annotation> annotation) {
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
		return entry;
	}

	@Override
	protected void doAutoDetection() throws DslException {
		Native n = this.type.getAnnotation(Native.class);
		if (n != null && n.allDeclaredClasses()) this.entry.setAllDeclaredClasses(true);
		if (n != null && n.allDeclaredFields()) this.entry.setAllDeclaredFields(true);
		if (n != null && n.allPublicClasses()) this.entry.setAllPublicClasses(true);
		if (n != null && n.queryAllDeclaredConstructors()) this.entry.setQueryAllDeclaredConstructors(true);
		if (n != null && n.queryAllDeclaredMethods()) this.entry.setQueryAllDeclaredMethods(true);
		if (n != null && n.queryAllPublicConstructors()) this.entry.setQueryAllPublicConstructors(true);
		if (n != null && n.queryAllPublicMethods()) this.entry.setQueryAllPublicMethods(false);

		Arrays.stream(this.type.getDeclaredFields()).filter(f -> f.getAnnotation(Native.class)!=null).forEach(this::field);
		Arrays.stream(this.type.getDeclaredConstructors()).filter(c -> c.getAnnotation(Native.class)!=null).forEach(this::constructor);
		Arrays.stream(this.type.getDeclaredMethods()).filter(m -> m.getAnnotation(Native.class)!=null).forEach(this::method);
	}

}