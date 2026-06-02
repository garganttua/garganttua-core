package com.garganttua.core.reflection.fields;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.TypeUtils;

/**
 * Static helpers for inspecting and instantiating fields: generic type
 * extraction, collection/map/array detection, primitive checks, and default
 * value instantiation, plus a process-wide class blacklist.
 */
public class Fields {
    private static final Logger log = Logger.getLogger(Fields.class);

	private Fields() {
		/* This utility class should not be instantiated */
	}

	/**
	 * Process-wide blacklist of classes that should be skipped during recursive
	 * field traversal (for example, to break otherwise infinite type cycles).
	 */
	public static class BlackList {
		private BlackList() {
			/* This utility class should not be instantiated */
		}

		private static List<IClass<?>> blackList = new CopyOnWriteArrayList<>();

		/**
		 * Adds a class to the blacklist.
		 *
		 * @param clazz the class to blacklist
		 */
		public static void addClassToBlackList(IClass<?> clazz) {
			log.debug("Adding class {} to blacklist", clazz.getName());
			BlackList.blackList.add(clazz);
		}

		/**
		 * Returns whether the given class is blacklisted.
		 *
		 * @param clazz the class to test
		 * @return {@code true} if the class is blacklisted
		 */
		public static boolean isBlackListed(IClass<?> clazz) {
			Optional<IClass<?>> found = BlackList.blackList.stream().filter(cl -> cl.equals(clazz)).findFirst();
			boolean blacklisted = found.isPresent();
			log.trace("Class {} blacklist check: {}", clazz.getName(), blacklisted);
			return blacklisted;
		}
	}

	/**
	 * Builds an ANSI-colored single-line representation of a field
	 * ({@code Owner.name : Type}) for console output.
	 *
	 * @param f the field to render
	 * @return the colored representation
	 */
	public static String prettyColored(IField f) {
		log.trace("Creating pretty colored representation for field: {}", f);
		return "\u001B[36m" + f.getDeclaringClass().getSimpleName() + "\u001B[0m"
				+ "."
				+ "\u001B[32m" + f.getName() + "\u001B[0m"
				+ " : "
				+ "\u001B[33m" + f.getType().getSimpleName() + "\u001B[0m";
	}

	/**
	 * Returns the generic type argument at the given index of a field's declared type.
	 *
	 * @param field the field whose generic type is inspected
	 * @param genericTypeIndex the zero-based type-argument index
	 * @return the resolved type argument, or {@code null} if none
	 */
	static public IClass<?> getGenericType(IField field, int genericTypeIndex) {
		log.trace("Getting generic type for field={}, index={}", field.getName(), genericTypeIndex);
		return getGenericType(field.getGenericType(), genericTypeIndex);
	}

	/**
	 * Returns the generic type argument at the given index of a field's declared
	 * type, resolving classes through the supplied provider.
	 *
	 * @param field the field whose generic type is inspected
	 * @param genericTypeIndex the zero-based type-argument index
	 * @param provider the reflection provider used for class resolution
	 * @return the resolved type argument, or {@code null} if none
	 */
	static public IClass<?> getGenericType(IField field, int genericTypeIndex, IReflectionProvider provider) {
		log.trace("Getting generic type for field={}, index={}", field.getName(), genericTypeIndex);
		return getGenericType(field.getGenericType(), genericTypeIndex, provider);
	}

	/**
	 * Returns the generic type argument at the given index of a class's generic superclass.
	 *
	 * @param clazz the class whose generic superclass is inspected
	 * @param genericTypeIndex the zero-based type-argument index
	 * @return the resolved type argument, or {@code null} if none
	 */
	static public IClass<?> getGenericType(IClass<?> clazz, int genericTypeIndex) {
		log.trace("Getting generic type for class={}, index={}", clazz.getName(), genericTypeIndex);
		return getGenericType(clazz.getGenericSuperclass(), genericTypeIndex);
	}

	/**
	 * Returns the generic type argument at the given index of a class's generic
	 * superclass, resolving classes through the supplied provider.
	 *
	 * @param clazz the class whose generic superclass is inspected
	 * @param genericTypeIndex the zero-based type-argument index
	 * @param provider the reflection provider used for class resolution
	 * @return the resolved type argument, or {@code null} if none
	 */
	static public IClass<?> getGenericType(IClass<?> clazz, int genericTypeIndex, IReflectionProvider provider) {
		log.trace("Getting generic type for class={}, index={}", clazz.getName(), genericTypeIndex);
		return getGenericType(clazz.getGenericSuperclass(), genericTypeIndex, provider);
	}

	private static IClass<?> getGenericType(Type type, int genericTypeIndex) {
		log.trace("Getting generic type from Type={}, index={}", type, genericTypeIndex);
		if (type instanceof ParameterizedType parameterizedType) {
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > genericTypeIndex && typeArguments[genericTypeIndex] instanceof Class<?> clz) {
				IClass<?> result = IClass.getClass(clz);
				log.debug("Found generic type: {}", result.getName());
				return result;
			}
		}
		log.debug("No generic type found for Type={}, index={}", type, genericTypeIndex);
		return null;
	}

	private static IClass<?> getGenericType(Type type, int genericTypeIndex, IReflectionProvider provider) {
		log.trace("Getting generic type from Type={}, index={}", type, genericTypeIndex);
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > genericTypeIndex && typeArguments[genericTypeIndex] instanceof Class<?>) {
				IClass<?> result = provider.getClass((Class<?>) typeArguments[genericTypeIndex]);
				log.debug("Found generic type: {}", result.getName());
				return result;
			}
		}
		log.debug("No generic type found for Type={}, index={}", type, genericTypeIndex);
		return null;
	}

	/**
	 * Returns whether the given class is not a primitive type.
	 *
	 * @param clazz the class to test
	 * @return {@code true} if the class is not primitive
	 */
	public static boolean isNotPrimitive(IClass<?> clazz) {
		return TypeUtils.isNotPrimitive(clazz);
	}

	/**
	 * Returns whether the given class is neither primitive nor a JDK-internal type
	 * (the latter being skipped during recursive field traversal).
	 *
	 * @param clazz the class to test
	 * @return {@code true} if the class is neither primitive nor internal
	 */
	public static boolean isNotPrimitiveOrInternal(IClass<?> clazz) {
		return TypeUtils.isNotPrimitiveOrInternal(clazz);
	}

	/**
	 * Returns whether the field's type is an array, {@link Collection}, or {@link Map}.
	 *
	 * @param field the field to test
	 * @return {@code true} for array, collection, or map fields
	 */
	public static boolean isArrayOrMapOrCollectionField(IField field) {
		log.trace("Checking if field {} is array/map/collection", field.getName());
		Class<?> rawType = (Class<?>) field.getType().getType();
		return Collection.class.isAssignableFrom(rawType) ||
				Map.class.isAssignableFrom(rawType) ||
				rawType.isArray();
	}

	/**
	 * Returns whether the field's type is an array, {@link Collection}, or
	 * {@link Map}, resolving the relevant classes through the supplied provider.
	 *
	 * @param field the field to test
	 * @param provider the reflection provider used for class resolution
	 * @return {@code true} for array, collection, or map fields
	 */
	public static boolean isArrayOrMapOrCollectionField(IField field, IReflectionProvider provider) {
		log.trace("Checking if field {} is array/map/collection", field.getName());
		IClass<?> type = field.getType();
		return type.isArray()
				|| provider.getClass(Collection.class).isAssignableFrom(type)
				|| provider.getClass(Map.class).isAssignableFrom(type);
	}

	/**
	 * Instantiates a value suitable for the given field: a fresh no-arg instance
	 * of its type, falling back to a default primitive value or an empty
	 * collection/map when direct instantiation fails.
	 *
	 * @param field the field whose type should be instantiated
	 * @return a non-null instance for the field's type
	 * @throws ReflectionException if no instance can be produced for the type
	 */
	public static Object instanciate(IField field) throws ReflectionException {
		log.trace("instanciate entry for field: {}", field.getName());
		log.debug("Instanciating Field Object of type {}", field.getType().getSimpleName());
		Object object = null;

		try {
			object = instanciateNewObject(field.getType());
			log.debug("Successfully instantiated object of type {}", field.getType().getSimpleName());
		} catch (IllegalArgumentException | SecurityException | ReflectionException e) {
			log.warn("Exception during instanciation: {}, trying instanciating supported interface object",
					e.getMessage());
			return Fields.instanciatePrimitiveOrInterfaceObjectOr(field);
		}

		return object;
	}

	private static <T> T instanciateNewObject(IClass<T> clazz) throws ReflectionException {
		try {
			var ctor = clazz.getDeclaredConstructor();
			ctor.setAccessible(true);
			return (T) ctor.newInstance();
		} catch (Exception e) {
			throw new ReflectionException("Class " + clazz.getSimpleName() + " does not have constructor with no params", e);
		}
	}

	private static Object instanciatePrimitiveOrInterfaceObjectOr(IField field) throws ReflectionException {
		log.debug("Attempting to instantiate primitive or interface for field type: {}",
				field.getType().getSimpleName());
		Class<?> rawType = (Class<?>) field.getType().getType();
		Object primitive = defaultPrimitiveOrArray(rawType);
		if (primitive != null) {
			return primitive;
		}
		Object collection = defaultCollectionInstance(rawType);
		if (collection != null) {
			return collection;
		}
		log.error("Unable to instanciate object of type {}", field.getType().getSimpleName());
		throw new ReflectionException("Unable to instanciate object of type " + field.getType().getSimpleName());
	}

	/** Default boxed value for a primitive type, or an empty array for array types; {@code null} otherwise. */
	private static Object defaultPrimitiveOrArray(Class<?> rawType) {
		if (rawType == int.class) {
			return 1;
		}
		if (rawType == long.class) {
			return 0L;
		}
		if (rawType == float.class) {
			return 0F;
		}
		if (rawType == double.class) {
			return 0D;
		}
		if (rawType == short.class) {
			return (short) 0;
		}
		if (rawType == byte.class) {
			return (byte) 0x00;
		}
		if (rawType == char.class) {
			return '0';
		}
		if (rawType == boolean.class) {
			return Boolean.FALSE;
		}
		if (rawType.isArray()) {
			return Array.newInstance(rawType.getComponentType(), 0);
		}
		return null;
	}

	/** Empty instance for a well-known collection/map interface type; {@code null} otherwise. */
	private static Object defaultCollectionInstance(Class<?> rawType) {
		if (Map.class.isAssignableFrom(rawType)) {
			return new HashMap<>();
		}
		if (List.class.isAssignableFrom(rawType)) {
			return new ArrayList<>();
		}
		if (Set.class.isAssignableFrom(rawType)) {
			return new HashSet<>();
		}
		if (Queue.class.isAssignableFrom(rawType)) {
			return new LinkedList<>();
		}
		if (Collection.class.isAssignableFrom(rawType)) {
			return new Vector<>();
		}
		return null;
	}
}
