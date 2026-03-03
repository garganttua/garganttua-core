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

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.TypeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Fields {

	private Fields() {
		/* This utility class should not be instantiated */
	}

	public static class BlackList {
		private BlackList() {
			/* This utility class should not be instantiated */
		}

		private static List<IClass<?>> blackList = new CopyOnWriteArrayList<>();

		public static void addClassToBlackList(IClass<?> clazz) {
			log.atDebug().log("Adding class {} to blacklist", clazz.getName());
			BlackList.blackList.add(clazz);
		}

		public static boolean isBlackListed(IClass<?> clazz) {
			Optional<IClass<?>> found = BlackList.blackList.stream().filter(cl -> cl.equals(clazz)).findFirst();
			boolean blacklisted = found.isPresent();
			log.atTrace().log("Class {} blacklist check: {}", clazz.getName(), blacklisted);
			return blacklisted;
		}
	}

	public static String prettyColored(IField f) {
		log.atTrace().log("Creating pretty colored representation for field: {}", f);
		return "\u001B[36m" + f.getDeclaringClass().getSimpleName() + "\u001B[0m"
				+ "."
				+ "\u001B[32m" + f.getName() + "\u001B[0m"
				+ " : "
				+ "\u001B[33m" + f.getType().getSimpleName() + "\u001B[0m";
	}

	static public IClass<?> getGenericType(IField field, int genericTypeIndex) {
		log.atTrace().log("Getting generic type for field={}, index={}", field.getName(), genericTypeIndex);
		return getGenericType(field.getGenericType(), genericTypeIndex);
	}

	static public IClass<?> getGenericType(IField field, int genericTypeIndex, IReflectionProvider provider) {
		log.atTrace().log("Getting generic type for field={}, index={}", field.getName(), genericTypeIndex);
		return getGenericType(field.getGenericType(), genericTypeIndex, provider);
	}

	static public IClass<?> getGenericType(IClass<?> clazz, int genericTypeIndex) {
		log.atTrace().log("Getting generic type for class={}, index={}", clazz.getName(), genericTypeIndex);
		return getGenericType(clazz.getGenericSuperclass(), genericTypeIndex);
	}

	static public IClass<?> getGenericType(IClass<?> clazz, int genericTypeIndex, IReflectionProvider provider) {
		log.atTrace().log("Getting generic type for class={}, index={}", clazz.getName(), genericTypeIndex);
		return getGenericType(clazz.getGenericSuperclass(), genericTypeIndex, provider);
	}

	private static IClass<?> getGenericType(Type type, int genericTypeIndex) {
		log.atTrace().log("Getting generic type from Type={}, index={}", type, genericTypeIndex);
		if (type instanceof ParameterizedType parameterizedType) {
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > genericTypeIndex && typeArguments[genericTypeIndex] instanceof Class<?> clz) {
				IClass<?> result = IClass.getClass(clz);
				log.atDebug().log("Found generic type: {}", result.getName());
				return result;
			}
		}
		log.atDebug().log("No generic type found for Type={}, index={}", type, genericTypeIndex);
		return null;
	}

	private static IClass<?> getGenericType(Type type, int genericTypeIndex, IReflectionProvider provider) {
		log.atTrace().log("Getting generic type from Type={}, index={}", type, genericTypeIndex);
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > genericTypeIndex && typeArguments[genericTypeIndex] instanceof Class<?>) {
				IClass<?> result = provider.getClass((Class<?>) typeArguments[genericTypeIndex]);
				log.atDebug().log("Found generic type: {}", result.getName());
				return result;
			}
		}
		log.atDebug().log("No generic type found for Type={}, index={}", type, genericTypeIndex);
		return null;
	}

	public static boolean isNotPrimitive(IClass<?> clazz) {
		return TypeUtils.isNotPrimitive(clazz);
	}

	public static boolean isNotPrimitiveOrInternal(IClass<?> clazz) {
		return TypeUtils.isNotPrimitiveOrInternal(clazz);
	}

	public static boolean isArrayOrMapOrCollectionField(IField field) {
		log.atTrace().log("Checking if field {} is array/map/collection", field.getName());
		Class<?> rawType = (Class<?>) field.getType().getType();
		return Collection.class.isAssignableFrom(rawType) ||
				Map.class.isAssignableFrom(rawType) ||
				rawType.isArray();
	}

	public static boolean isArrayOrMapOrCollectionField(IField field, IReflectionProvider provider) {
		log.atTrace().log("Checking if field {} is array/map/collection", field.getName());
		IClass<?> type = field.getType();
		return type.isArray()
				|| provider.getClass(Collection.class).isAssignableFrom(type)
				|| provider.getClass(Map.class).isAssignableFrom(type);
	}

	public static Object instanciate(IField field) throws ReflectionException {
		log.atTrace().log("instanciate entry for field: {}", field.getName());
		log.atDebug().log("Instanciating Field Object of type {}", field.getType().getSimpleName());
		Object object = null;

		try {
			object = instanciateNewObject(field.getType());
			log.atDebug().log("Successfully instantiated object of type {}", field.getType().getSimpleName());
		} catch (IllegalArgumentException | SecurityException | ReflectionException e) {
			log.atWarn().log("Exception during instanciation: {}, trying instanciating supported interface object",
					e.getMessage());
			return Fields.instanciatePrimitiveOrInterfaceObjectOr(field);
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	private static <T> T instanciateNewObject(IClass<T> clazz) throws ReflectionException {
		try {
			var ctor = clazz.getDeclaredConstructor();
			ctor.setAccessible(true);
			return (T) ctor.newInstance();
		} catch (Exception e) {
			throw new ReflectionException("Class " + clazz.getSimpleName() + " does not have constructor with no params", e);
		}
	}

	@SuppressWarnings("rawtypes")
	private static Object instanciatePrimitiveOrInterfaceObjectOr(IField field) throws ReflectionException {
		log.atDebug().log("Attempting to instantiate primitive or interface for field type: {}",
				field.getType().getSimpleName());
		Class<?> rawType = (Class<?>) field.getType().getType();
		if (rawType == int.class) {
			return (int) 1;
		}
		if (rawType == long.class) {
			return (long) 0L;
		}
		if (rawType == float.class) {
			return (float) 0F;
		}
		if (rawType == double.class) {
			return (double) 0D;
		}
		if (rawType == short.class) {
			return (short) 0;
		}
		if (rawType == byte.class) {
			return (byte) 0x00;
		}
		if (rawType == char.class) {
			return (char) '0';
		}
		if (rawType == boolean.class) {
			return (boolean) false;
		}
		if (rawType.isArray()) {
			return Array.newInstance(rawType.getComponentType(), 0);
		}
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
		log.atError().log("Unable to instanciate object of type {}", field.getType().getSimpleName());
		throw new ReflectionException("Unable to instanciate object of type " + field.getType().getSimpleName());
	}
}
