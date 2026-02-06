package com.garganttua.core.reflection.fields;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Fields {

	public static class BlackList {
		private static List<Class<?>> blackList = new CopyOnWriteArrayList<>();

		public static void addClassToBlackList(Class<?> clazz) {
			log.atDebug().log("Adding class {} to blacklist", clazz.getName());
			BlackList.blackList.add(clazz);
		}

		public static boolean isBlackListed(Class<?> clazz) {
			Optional<Class<?>> found = BlackList.blackList.stream().filter(cl -> cl.equals(clazz)).findFirst();
			boolean blacklisted = found.isPresent();
			log.atTrace().log("Class {} blacklist check: {}", clazz.getName(), blacklisted);
			return blacklisted;
		}
	}

	public static String prettyColored(Field f) {
		log.atTrace().log("Creating pretty colored representation for field: {}", f);
		return "\u001B[36m" + f.getDeclaringClass().getSimpleName() + "\u001B[0m"
				+ "."
				+ "\u001B[32m" + f.getName() + "\u001B[0m"
				+ " : "
				+ "\u001B[33m" + f.getType().getSimpleName() + "\u001B[0m";
	}

	static public Class<?> getGenericType(Field field, int genericTypeIndex) {
		log.atTrace().log("Getting generic type for field={}, index={}", field.getName(), genericTypeIndex);
		return getGenericType(field.getGenericType(), genericTypeIndex);
	}

	static public Class<?> getGenericType(Class<?> clazz, int genericTypeIndex) {
		log.atTrace().log("Getting generic type for class={}, index={}", clazz.getName(), genericTypeIndex);
		return getGenericType(clazz.getGenericSuperclass(), genericTypeIndex);
	}

	private static Class<?> getGenericType(Type type, int genericTypeIndex) {
		log.atTrace().log("Getting generic type from Type={}, index={}", type, genericTypeIndex);
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > 0 && typeArguments[genericTypeIndex] instanceof Class<?>) {
				Class<?> result = (Class<?>) typeArguments[genericTypeIndex];
				log.atDebug().log("Found generic type: {}", result.getName());
				return result;
			}
		}
		log.atDebug().log("No generic type found for Type={}, index={}", type, genericTypeIndex);
		return null;
	}

	public static boolean isNotPrimitive(Class<?> clazz) {
		log.atTrace().log("Checking if class {} is not primitive", clazz.getName());
		if (clazz.isPrimitive()) {
			return false;
		}

		if (clazz == Integer.class || clazz == Long.class || clazz == Float.class || clazz == Double.class
				|| clazz == Short.class || clazz == Byte.class || clazz == Character.class || clazz == Boolean.class
				|| clazz == String.class || clazz == Date.class) {
			return false;
		}
		return true;
	}

	public static boolean isNotPrimitiveOrInternal(Class<?> clazz) {
		if (!isNotPrimitive(clazz)) {
			return false;
		}
		Package package1 = clazz.getPackage();
		if (package1 == null) {
			return false;
		}

		String packageName = package1.getName();
		// Exclude all JDK internal packages
		if (packageName.startsWith("java.") || packageName.startsWith("javax.")
				|| packageName.startsWith("sun.") || packageName.startsWith("jdk.")) {
			return false;
		}

		return true;
	}

	public static boolean isArrayOrMapOrCollectionField(Field field) {
		log.atTrace().log("Checking if field {} is array/map/collection", field.getName());
		return Collection.class.isAssignableFrom(field.getType()) ||
				Map.class.isAssignableFrom(field.getType()) ||
				field.getType().isArray();
	}

	public static Object instanciate(Field field) throws ReflectionException {
		log.atTrace().log("instanciate entry for field: {}", field.getName());
		log.atDebug().log("Instanciating Field Object of type {}", field.getType().getSimpleName());
		Object object = null;

		try {
			object = ObjectReflectionHelper.instanciateNewObject(field.getType());
			log.atDebug().log("Successfully instantiated object of type {}", field.getType().getSimpleName());
		} catch (IllegalArgumentException | SecurityException | ReflectionException e) {
			log.atWarn().log("Exception during instanciation: {}, trying instanciating supported interface object", e.getMessage());
			return Fields.instanciatePrimitiveOrInterfaceObjectOr(field);
		}

		return object;
	}

	private static Object instanciatePrimitiveOrInterfaceObjectOr(Field field) throws ReflectionException {
		log.atDebug().log("Attempting to instantiate primitive or interface for field type: {}", field.getType().getSimpleName());
		if (field.getType() == int.class) {
			log.atDebug().log("Instantiating int primitive");
			return (int) 1;
		}
		if (field.getType() == long.class) {
			log.atDebug().log("Instantiating long primitive");
			return (long) 0L;
		}
		if (field.getType() == float.class) {
			log.atDebug().log("Instantiating float primitive");
			return (float) 0F;
		}
		if (field.getType() == double.class) {
			log.atDebug().log("Instantiating double primitive");
			return (double) 0D;
		}
		if (field.getType() == short.class) {
			log.atDebug().log("Instantiating short primitive");
			return (short) 0;
		}
		if (field.getType() == byte.class) {
			log.atDebug().log("Instantiating byte primitive");
			return (byte) 0x00;
		}
		if (field.getType() == char.class) {
			log.atDebug().log("Instantiating char primitive");
			return (char) '0';
		}
		if (field.getType() == boolean.class) {
			log.atDebug().log("Instantiating boolean primitive");
			return (boolean) false;
		}
		if (field.getType().isArray()) {
			log.atDebug().log("Instantiating array");
			return Array.newInstance(field.getType().getComponentType(), 0);
		}
		if (Map.class.isAssignableFrom(field.getType())) {
			log.atDebug().log("Instantiating Map");
			return ObjectReflectionHelper.newHashMapOf(Fields.getGenericType(field, 0),
					Fields.getGenericType(field, 1));
		}
		if (List.class.isAssignableFrom(field.getType())) {
			log.atDebug().log("Instantiating List");
			return ObjectReflectionHelper.newArrayListOf(Fields.getGenericType(field, 0));
		}
		if (Set.class.isAssignableFrom(field.getType())) {
			log.atDebug().log("Instantiating Set");
			return ObjectReflectionHelper.newHashSetOf(Fields.getGenericType(field, 0));
		}
		if (Queue.class.isAssignableFrom(field.getType())) {
			log.atDebug().log("Instantiating Queue");
			return ObjectReflectionHelper.newLinkedlistOf(Fields.getGenericType(field, 0));
		}
		if (Collection.class.isAssignableFrom(field.getType())) {
			log.atDebug().log("Instantiating Collection");
			return ObjectReflectionHelper.newVectorOf(Fields.getGenericType(field, 0));
		}
		log.atError().log("Unable to instanciate object of type {}", field.getType().getSimpleName());
		throw new ReflectionException("Unable to instanciate object of type " + field.getType().getSimpleName());
	}
}
