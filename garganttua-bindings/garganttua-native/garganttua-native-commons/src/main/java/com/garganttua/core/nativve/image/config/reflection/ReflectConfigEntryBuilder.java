package com.garganttua.core.nativve.image.config.reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflectionProvider;

/**
 * Fluent builder for a {@link ReflectConfigEntry}, registering fields, methods,
 * constructors and bulk-reflection flags for a single class. Supports automatic
 * detection of members annotated with {@link Reflected @Reflected}.
 */
public class ReflectConfigEntryBuilder extends AbstractAutomaticBuilder<IReflectionConfigurationEntryBuilder, IReflectionConfigurationEntry> implements IReflectionConfigurationEntryBuilder {
    private static final Logger log = Logger.getLogger(ReflectConfigEntryBuilder.class);

	private final IReflectionConfigurationEntry entry;
	private IClass<?> type;

	private static volatile IReflectionProvider cachedDefaultProvider;

	private static <T> IClass<T> wrapClass(Class<T> clazz) {
		return defaultProvider().getClass(clazz);
	}

	private static IReflectionProvider defaultProvider() {
		if (cachedDefaultProvider != null) {
			return cachedDefaultProvider;
		}
		try {
			Class<?> providerClass = Class.forName("com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
			cachedDefaultProvider = (IReflectionProvider) providerClass.getDeclaredConstructor().newInstance();
			return cachedDefaultProvider;
		} catch (Exception e) {
			throw new IllegalStateException("No IReflectionProvider available. Ensure garganttua-runtime-reflection is on the classpath.", e);
		}
	}

	private static final IClass<Reflected> REFLECTED_CLASS = wrapClass(Reflected.class);

	/**
	 * Creates a builder for the given type, starting from an empty entry.
	 *
	 * @param type the class to configure for reflection; must not be {@code null}
	 */
	public ReflectConfigEntryBuilder(IClass<?> type) {
		log.trace("Creating ReflectConfigEntryBuilder for type: {}", type.getName());
		this.type = Objects.requireNonNull(type, "Type cannot be null");
		this.entry = new ReflectConfigEntry(type.getName());
		this.entry.setFields(new ArrayList<>());
		this.entry.setMethods(new ArrayList<>());
		log.debug("Initialized ReflectConfigEntryBuilder for: {}", type.getName());
	}

	/**
	 * Creates a builder that wraps an existing entry, resolving its backing class.
	 *
	 * @param entry the pre-existing entry to continue configuring
	 * @throws DslException if the entry's class cannot be loaded from the classpath
	 */
	public ReflectConfigEntryBuilder(IReflectionConfigurationEntry entry) throws DslException {
		log.trace("Creating ReflectConfigEntryBuilder from existing entry: {}", entry.getName());
		this.entry = entry;
		try {
			this.type = entry.getEntryClass();
			log.debug("Loaded entry class: {}", this.type.getName());
		} catch (ClassNotFoundException e) {
			log.error("Failed to load entry class: {}", entry.getName());
			throw new DslException(e);
		}
	}

	/**
	 * Creates a builder for the given class.
	 *
	 * @param clazz the class to configure for reflection
	 * @return a new builder starting from an empty entry
	 */
	public static IReflectionConfigurationEntryBuilder builder(IClass<?> clazz) {
		return new ReflectConfigEntryBuilder(clazz);
	}

	/**
	 * Creates a builder that continues configuring an existing entry.
	 *
	 * @param entry the entry to wrap
	 * @return a new builder backed by {@code entry}
	 * @throws DslException if the entry's class cannot be loaded from the classpath
	 */
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
		log.trace("Adding field: {}", fieldName);
		if (entry.getFields().stream().noneMatch(field -> field.getName().equals(fieldName))) {
			ReflectConfigEntry.Field field = new ReflectConfigEntry.Field();
			field.setName(fieldName);
			entry.getFields().add(field);
			log.debug("Added field to reflection config: {}", fieldName);
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder field(IField field) {
		return field(field.getName());
	}

	@Override
	public IReflectionConfigurationEntryBuilder method(String methodName, IClass<?>... parameterTypes) {
		log.trace("Adding method: {} with {} parameters", methodName, parameterTypes.length);
		List<String> paramNames = Arrays.stream(parameterTypes).map(IClass::getName).collect(Collectors.toList());

		if (entry.getMethods().stream()
				.noneMatch(m -> m.getName().equals(methodName) && m.getParameterTypes().equals(paramNames))) {
			ReflectConfigEntry.Method method = new ReflectConfigEntry.Method();
			method.setName(methodName);
			method.setParameterTypes(paramNames);
			entry.getMethods().add(method);
			log.debug("Added method to reflection config: {}", methodName);
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder method(IMethod method) {
		return method(method.getName(), method.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder constructor(String constructorName, IClass<?>... parameterTypes) {
		log.trace("Adding constructor with {} parameters", parameterTypes.length);
		return method("<init>", parameterTypes);
	}

	@Override
	public IReflectionConfigurationEntryBuilder constructor(IConstructor<?> ctor) {
		return constructor("<init>", ctor.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder fieldsAnnotatedWith(IClass<? extends Annotation> annotation) {
		log.trace("Adding fields annotated with: {}", annotation.getName());
		try {
			for (IField field : this.entry.getEntryClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(annotation)) {
					field(field.getName());
				}
			}
			log.debug("Added all fields annotated with: {}", annotation.getName());
		} catch (SecurityException | ClassNotFoundException e) {
			log.warn("Error processing fields annotated with {}: {}", annotation.getName(), e.getMessage());
		}
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder methodsAnnotatedWith(IClass<? extends Annotation> annotation) {
		log.trace("Adding methods annotated with: {}", annotation.getName());
		try {
			for (IMethod method : this.entry.getEntryClass().getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotation)) {
					method(method);
				}
			}
			log.debug("Added all methods annotated with: {}", annotation.getName());
		} catch (SecurityException | ClassNotFoundException e) {
			log.warn("Error processing methods annotated with {}: {}", annotation.getName(), e.getMessage());
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
	public IReflectionConfigurationEntryBuilder removeField(IField field) {
		return removeField(field.getName());
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeMethod(String methodName, IClass<?>... parameterTypes) {
		List<String> paramNames = Arrays.stream(parameterTypes).map(IClass::getName).collect(Collectors.toList());
		entry.setMethods(entry.getMethods().stream().filter(method -> !method.getName().equals(methodName)
				|| !method.getParameterTypes().equals(paramNames))
				.collect(Collectors.toList()));
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeMethod(IMethod method) {
		return removeMethod(method.getName(), method.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeConstructor(String constructorName, IClass<?>... parameterTypes) {
		return removeMethod("<init>", parameterTypes);
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeConstructor(IConstructor<?> ctor) {
		return removeConstructor("<init>", ctor.getParameterTypes());
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeFieldsAnnotatedWith(IClass<? extends Annotation> annotation) {
		entry.setFields(entry.getFields().stream().filter(field -> {
			try {
				IField f = this.entry.getEntryClass().getDeclaredField(field.getName());
				return !f.isAnnotationPresent(annotation);
			} catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
				return true;
			}
		}).collect(Collectors.toList()));
		return this;
	}

	@Override
	public IReflectionConfigurationEntryBuilder removeMethodAnnotatedWith(IClass<? extends Annotation> annotation) {
		entry.setMethods(entry.getMethods().stream().filter(method -> {
			try {
				IMethod m = this.entry.getEntryClass().getDeclaredMethod(method.getName());
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
		log.trace("Building reflection configuration entry for: {}", entry.getName());
		log.debug("Reflection configuration entry built for: {}", entry.getName());
		return entry;
	}

	@Override
	protected void doAutoDetection() throws DslException {
		log.trace("Starting auto-detection for type: {}", this.type.getName());
		Reflected r = this.type.getAnnotation(REFLECTED_CLASS);
		if (r != null && r.allDeclaredClasses()) {
			log.debug("Enabling allDeclaredClasses for: {}", this.type.getName());
			this.entry.setAllDeclaredClasses(true);
		}
		if (r != null && r.allDeclaredFields()) {
			log.debug("Enabling allDeclaredFields for: {}", this.type.getName());
			this.entry.setAllDeclaredFields(true);
		}
		if (r != null && r.allPublicClasses()) {
			log.debug("Enabling allPublicClasses for: {}", this.type.getName());
			this.entry.setAllPublicClasses(true);
		}
		if (r != null && r.queryAllDeclaredConstructors()) {
			log.debug("Enabling queryAllDeclaredConstructors for: {}", this.type.getName());
			this.entry.setQueryAllDeclaredConstructors(true);
		}
		if (r != null && r.queryAllDeclaredMethods()) {
			log.debug("Enabling queryAllDeclaredMethods for: {}", this.type.getName());
			this.entry.setQueryAllDeclaredMethods(true);
		}
		if (r != null && r.queryAllPublicConstructors()) {
			log.debug("Enabling queryAllPublicConstructors for: {}", this.type.getName());
			this.entry.setQueryAllPublicConstructors(true);
		}
		if (r != null && r.queryAllPublicMethods()) {
			log.debug("Enabling queryAllPublicMethods for: {}", this.type.getName());
			this.entry.setQueryAllPublicMethods(false);
		}

		log.debug("Detecting @Reflected annotated fields, constructors, and methods for: {}", this.type.getName());
		Arrays.stream(this.type.getDeclaredFields()).filter(f -> f.getAnnotation(REFLECTED_CLASS)!=null).forEach(this::field);
		Arrays.stream(this.type.getDeclaredConstructors()).filter(c -> c.getAnnotation(REFLECTED_CLASS)!=null).forEach(this::constructor);
		Arrays.stream(this.type.getDeclaredMethods()).filter(m -> m.getAnnotation(REFLECTED_CLASS)!=null).forEach(this::method);
		log.trace("Completed auto-detection for type: {}", this.type.getName());
	}

}
