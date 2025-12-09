package com.garganttua.core.reflection.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectReflectionHelper {

	public static IAnnotationScanner annotationScanner;

	private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
	static {
		PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
		PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
		PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
		PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
		PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
		PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
		PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
		PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
		PRIMITIVE_TO_WRAPPER.put(void.class, Void.class);
	}

	public static Constructor<?> getConstructorWithNoParams(Class<?> classs) {
		log.atTrace().log("Getting no-param constructor for class: {}", classs.getName());
		try {
			Constructor<?> ctor = classs.getDeclaredConstructor();
			log.atDebug().log("Found no-param constructor for class: {}", classs.getName());
			return ctor;
		} catch (NoSuchMethodException | SecurityException e) {
			log.atDebug().log("No no-param constructor found for class: {}", classs.getName());
			return null;
		}
	}

	public static Constructor<?> getConstructor(Class<?> classs, Class<?>... params) {
		log.atTrace().log("Getting constructor for class: {} with {} parameters", classs.getName(), params.length);
		for (Constructor<?> ctor : classs.getDeclaredConstructors()) {
			Class<?>[] pts = ctor.getParameterTypes();
			if (pts.length != params.length)
				continue;

			boolean ok = true;
			for (int i = 0; i < pts.length; i++) {
				Class<?> formal = pts[i];
				Class<?> actual = params[i];

				if (actual == null) {
					if (formal.isPrimitive()) {
						ok = false;
						break;
					}
				} else {
					if (!isAssignable(formal, actual)) {
						ok = false;
						break;
					}
				}
			}
			if (ok) {
				log.atDebug().log("Found matching constructor for class: {}", classs.getName());
				return ctor;
			}
		}
		log.atDebug().log("No matching constructor found for class: {}", classs.getName());
		return null;
	}

	private static boolean isAssignable(Class<?> formal, Class<?> actual) {
		if (formal.isAssignableFrom(actual))
			return true;

		if (formal.isPrimitive()) {
			Class<?> wrap = PRIMITIVE_TO_WRAPPER.get(formal);
			return wrap != null && wrap.equals(actual);
		}

		if (actual.isPrimitive()) {
			Class<?> wrapActual = PRIMITIVE_TO_WRAPPER.get(actual);
			return wrapActual != null && formal.isAssignableFrom(wrapActual);
		}
		return false;
	}

	public static Field getField(Class<?> objectClass, String fieldName) {
		log.atTrace().log("Getting field {} from class: {}", fieldName, objectClass.getName());
		for (Field f : objectClass.getDeclaredFields()) {
			if (f.getName().equals(fieldName)) {
				log.atDebug().log("Found field {} in class: {}", fieldName, objectClass.getName());
				return f;
			}
		}
		if (objectClass.getSuperclass() != null) {
			return ObjectReflectionHelper.getField(objectClass.getSuperclass(), fieldName);
		}
		log.atDebug().log("Field {} not found in class: {}", fieldName, objectClass.getName());
		return null;
	}

	public static Method getMethod(Class<?> objectClass, String methodName) {
		log.atTrace().log("Getting method {} from class: {}", methodName, objectClass.getName());
		for (Method f : objectClass.getDeclaredMethods()) {
			if (f.getName().equals(methodName)) {
				log.atDebug().log("Found method {} in class: {}", methodName, objectClass.getName());
				return f;
			}
		}
		if (objectClass.getSuperclass() != null) {
			return getMethod(objectClass.getSuperclass(), methodName);
		}
		log.atDebug().log("Method {} not found in class: {}", methodName, objectClass.getName());
		return null;
	}

	public static <destination> destination instanciateNewObject(Class<destination> clazz, Object... params)
			throws ReflectionException {
		log.atTrace().log("Instantiating new object of class: {} with {} params", clazz.getName(), params != null ? params.length : 0);
		if (params == null || params.length == 0)
			instanciateNewObject(clazz);

		Class<Object>[] paramTypes = (Class<Object>[]) Arrays.stream(params).map(p -> p.getClass())
				.collect(Collectors.toList()).toArray(new Class<?>[1]);
		Constructor<?> ctor = ObjectReflectionHelper.getConstructor(clazz, paramTypes);
		if (ctor != null) {
			try (ConstructorAccessManager accessor = new ConstructorAccessManager(ctor)) {
				destination result = (destination) ctor.newInstance(params);
				log.atInfo().log("Successfully instantiated new object of class: {}", clazz.getName());
				return result;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				log.atError().log("Error instantiating object of class: {}", clazz.getName(), e);
				throw new ReflectionException(e);
			}
		}

		log.atError().log("Class {} does not have constructor with provided params", clazz.getName());
		throw new ReflectionException(
				"Class " + clazz.getSimpleName() + " does not have constructor with params " + params);
	}

	public static <destination> destination instanciateNewObject(Class<destination> clazz) throws ReflectionException {
		log.atTrace().log("Instantiating new object of class: {} with no params", clazz.getName());
		Constructor<?> ctor = ObjectReflectionHelper.getConstructorWithNoParams(clazz);
		if (ctor != null) {
			try (ConstructorAccessManager accessor = new ConstructorAccessManager(ctor)) {
				destination result = (destination) ctor.newInstance();
				log.atInfo().log("Successfully instantiated new object of class: {}", clazz.getName());
				return result;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				log.atError().log("Error instantiating object of class: {}", clazz.getName(), e);
				throw new ReflectionException(e);
			}
		}
		log.atError().log("Class {} does not have constructor with no params", clazz.getName());
		throw new ReflectionException("Class " + clazz.getSimpleName() + " does not have constructor with no params");
	}

	static public void setObjectFieldValue(Object entity, Field field, Object value) throws ReflectionException {
		log.atTrace().log("Setting field {} on object of type {}", field != null ? field.getName() : "null", entity.getClass().getName());
		if (field == null) {
			log.atError().log("Cannot set null field of object {}", entity.getClass().getName());
			throw new ReflectionException(
					"Cannot set null field of object " + entity.getClass().getName() + " with value " + value);
		}

		try (FieldAccessManager manager = new FieldAccessManager(field)) {
			field.set(entity, value);
			log.atDebug().log("Successfully set field {} on object of type {}", field.getName(), entity.getClass().getName());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.atError().log("Cannot set field {} of object {}", field.getName(), entity.getClass().getName(), e);
			throw new ReflectionException("Cannot set field " + field.getName() + " of object "
					+ entity.getClass().getName() + " with value " + value, e);
		}
	}


    /**
     * Helper method to extract a Class from a Type.
     *
     * @param type the Type to extract from
     * @return the extracted Class
     */
    public static Class<?> extractClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }

        if (type instanceof ParameterizedType) {
            return extractClass(((ParameterizedType) type).getRawType());
        }

        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = extractClass(componentType);
            return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
        }

        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? Object.class : extractClass(bounds[0]);
        }

        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return upperBounds.length == 0 ? Object.class : extractClass(upperBounds[0]);
        }

        throw new IllegalArgumentException("Impossible de convertir en Class<?> : " + type);
    }

	public static Object getObjectFieldValue(Object entity, String fieldName) throws ReflectionException {
		log.atTrace().log("Getting field {} from object of type {}", fieldName, entity.getClass().getName());
		Field field = ObjectReflectionHelper.getField(entity.getClass(), fieldName);
		if (field == null) {
			log.atError().log("Cannot get field {} of object {}", fieldName, entity.getClass().getName());
			throw new ReflectionException(
					"Cannot get field " + fieldName + " of object " + entity.getClass().getName());
		}

		return getObjectFieldValue(entity, field);
	}

	public static Object getObjectFieldValue(Object entity, Field field) throws ReflectionException {
		log.atTrace().log("Getting field {} from object of type {}", field.getName(), entity.getClass().getName());
		try (FieldAccessManager manager = new FieldAccessManager(field)) {
			Object value = field.get(entity);
			log.atDebug().log("Successfully got field {} value from object of type {}", field.getName(), entity.getClass().getName());
			return value;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.atError().log("Cannot get field {} of object {}", field.getName(), entity.getClass().getName(), e);
			throw new ReflectionException(
					"Cannot get field " + field.getName() + " of object " + entity.getClass().getName(), e);
		}
	}

	public static Object invokeMethod(Object object, String methodName, Method method, Object... args)
			throws ReflectionException {
		log.atTrace().log("Invoking method {} on object of type {} with {} args", methodName, object.getClass().getName(), args.length);
		ObjectReflectionHelper.checkMethodAndParams(method, args);

		try (MethodAccessManager manager = new MethodAccessManager(method)) {
			Object result = method.invoke(object, args);
			log.atDebug().log("Successfully invoked method {} on object of type {}", methodName, object.getClass().getName());
			return result;
		} catch (IllegalAccessException | InvocationTargetException e) {
			log.atError().log("Cannot invoke method {} of object {}", methodName, object.getClass().getName(), e);
			throw new ReflectionException(
					"Cannot invoke method " + methodName + " of object " + object.getClass().getName(), e.getCause());
		}
	}

	public static void checkMethodAndParams(Method method, Object... args) throws ReflectionException {
		if (method.getParameterCount() != args.length) {
			throw new ReflectionException("Method " + method.getName() + " needs " + method.getParameterCount() + " "
					+ method.getParameterTypes() + " but " + args.length + " have been provided : " + args);
		}

		Class<?>[] params = method.getParameterTypes();
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				continue;
			if (!equals(params[i], args[i].getClass())) {
				throw new ReflectionException("Method " + method.getName() + " needs parameter " + i
						+ " to be of type " + params[i] + ", not " + args[i].getClass());
			}
		}
	}

	public static <K, V> Map<K, V> newHashMapOf(Class<K> keyType, Class<V> valueType) {
		return new HashMap<K, V>();
	}

	public static <K> ArrayList<K> newArrayListOf(Class<K> type) {
		return new ArrayList<K>();
	}

	public static <K> HashSet<K> newHashSetOf(Class<K> type) {
		return new HashSet<K>();
	}

	public static <K> LinkedList<K> newLinkedlistOf(Class<K> type) {
		return new LinkedList<K>();
	}

	public static <K> Vector<K> newVectorOf(Class<?> type) {
		return new Vector<K>();
	}

	public static boolean isImplementingInterface(Class<?> interfaceType, Class<?> objectType) {
		List<Class<?>> interfaces = Arrays.asList(objectType.getInterfaces());
		Optional<Class<?>> found = interfaces.parallelStream().filter(c -> c.equals(interfaceType)).findFirst();
		return found.isPresent();
	}

	public static List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
		return ObjectReflectionHelper.annotationScanner.getClassesWithAnnotation(package_, annotation);
	}

	public static String getMethodAddressAnnotatedWith(Class<?> entityClass,
			Class<? extends Annotation> methodAnnotation) throws ReflectionException {

		Method found = getMethodAnnotatedWith(entityClass, methodAnnotation);

		if (found != null)
			return found.getName();

		return null;
	}

	public static Method getMethodAnnotatedWith(Class<?> entityClass, Class<? extends Annotation> methodAnnotation) {
		Method method = null;
		for (Method m : entityClass.getDeclaredMethods()) {
			if (m.isAnnotationPresent(methodAnnotation)) {
				method = m;
				break;
			}
		}

		if (method == null && entityClass.getSuperclass() != null) {
			method = getMethodAnnotatedWith(entityClass.getSuperclass(),
					methodAnnotation);
		}
		return method;
	}

	public static void checkMethodParamsHaveGoodTypes(Class<?> entityClass, Method method, Type returnedType,
			Type... methodParameters) throws ReflectionException {
		Type[] parameters = method.getGenericParameterTypes();

		if (parameters.length != methodParameters.length) {
			throw new ReflectionException("Entity " + entityClass.getSimpleName() + " has method "
					+ method.getName() + " but wrong parameters number");
		}

		for (int i = 0; i < methodParameters.length; i++) {
			if (!equals(methodParameters[i], parameters[i])) {
				throw new ReflectionException("Entity " + entityClass.getSimpleName() + " has method "
						+ method.getName() + " but parameter " + i + " is not of type "
						+ methodParameters[i].getTypeName());
			}
		}

		if (returnedType != null) {
			if (!method.getReturnType().isAssignableFrom(getClassFromType(returnedType))) {
				throw new ReflectionException("Entity " + entityClass.getSimpleName() + " has method "
						+ method.getName() + " but returned type is not " + returnedType.getTypeName() + " but is "
						+ method.getGenericReturnType());
			}
		}
	}

	public static String getMethodAddressAnnotatedWithAndCheckMethodParamsHaveGoodTypes(Class<?> entityClass,
			Class<? extends Annotation> methodAnnotation, Type returnedType, Type... methodParameters)
			throws ReflectionException {
		Method found = getMethodAnnotatedWith(entityClass, methodAnnotation);

		if (found != null) {
			checkMethodParamsHaveGoodTypes(entityClass, found, returnedType, methodParameters);
			return found.getName();
		}

		return null;
	}

	static public boolean isMapOfString(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			return parameterizedType.getRawType() == Map.class && typeArguments.length == 2
					&& typeArguments[0] == String.class && typeArguments[1] == String.class;
		}
		return false;
	}

	public static ParameterizedType getParameterizedType(Class<?> rawType, Type... typeArguments) {
		return new ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return typeArguments;
			}

			@Override
			public Type getRawType() {
				return rawType;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}
		};
	}

	public static String getFieldAddressAnnotatedWithAndCheckType(Class<?> entityClass,
			Class<? extends Annotation> annotationClass, Type fieldType) throws ReflectionException {
		log.atDebug().log("Looking for annotation " + annotationClass.getSimpleName() + " on field of type "
				+ getClassFromType(fieldType).getSimpleName() + " from type " + entityClass.getSimpleName());
		String fieldAddress = null;
		for (Field field : entityClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(annotationClass)) {
				if (fieldAddress != null && !fieldAddress.isEmpty()) {
					throw new ReflectionException("Entity " + entityClass.getSimpleName()
							+ " has more than one field annotated with " + annotationClass);
				}
				if (equals(field.getGenericType(), fieldType)) {
					fieldAddress = field.getName();
					break;
				} else {
					throw new ReflectionException(
							"Entity " + entityClass.getSimpleName() + " has field " + field.getName()
									+ " with wrong type " + field.getType().getName() + ", should be "
									+ getClassFromType(fieldType));
				}
			} else {
				if (Fields.isNotPrimitiveOrInternal(field.getType()) && !entityClass.equals(field.getType())) {
					fieldAddress = getFieldAddressAnnotatedWithAndCheckType(field.getType(), annotationClass,
							fieldType);
					if (fieldAddress != null)
						break;
				}
			}
		}

		if (fieldAddress == null && entityClass.getSuperclass() != null) {
			fieldAddress = getFieldAddressAnnotatedWithAndCheckType(entityClass.getSuperclass(), annotationClass,
					fieldType);
		}

		return fieldAddress;
	}

	/**
	 * 
	 * @param entityClass
	 * @param annotation
	 * @param linkedAnnotation : check in field objects only if the annotation is
	 *                         found for field
	 * @return
	 */
	public static List<String> getFieldAddressesWithAnnotation(Class<?> entityClass,
			Class<? extends Annotation> annotation, boolean linkedAnnotation) {
		List<String> fieldsAddresses = new ArrayList<String>();
		getFieldAddressesWithAnnotation(fieldsAddresses, entityClass, annotation, linkedAnnotation);
		return fieldsAddresses;
	}

	private static void getFieldAddressesWithAnnotation(List<String> fieldsAddresses, Class<?> entityClass,
			Class<? extends Annotation> annotation, boolean linkedAnnotation) {
		for (Field field : entityClass.getDeclaredFields()) {
			boolean found = false;
			if (field.isAnnotationPresent(annotation)) {
				fieldsAddresses.add(field.getName());
				found = true;
			}
			if (((found && linkedAnnotation) || !linkedAnnotation)
					&& Fields.isNotPrimitiveOrInternal(field.getType())
					&& !entityClass.equals(field.getType())) {
				getFieldAddressesWithAnnotation(fieldsAddresses, field.getType(), annotation, linkedAnnotation);
			}

			if (((found && linkedAnnotation) || !linkedAnnotation)
					&& Fields.isArrayOrMapOrCollectionField(field)) {
				int i = 0;

				Class<?> genericType = Fields.getGenericType(field.getType(), i);
				while (genericType != null) {
					getFieldAddressesWithAnnotation(fieldsAddresses, genericType, annotation, linkedAnnotation);
					i++;
					genericType = Fields.getGenericType(field.getType(), i);
				}
				;
			}
		}
		if (entityClass.getSuperclass() != null) {
			getFieldAddressesWithAnnotation(fieldsAddresses, entityClass.getSuperclass(), annotation, linkedAnnotation);
		}
	}

	public static Class<?> getClassFromType(Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		} else if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) type).getGenericComponentType();
			Class<?> componentClass = getClassFromType(componentType);
			return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
		} else {
			throw new IllegalArgumentException("Cannot extract Class<?> from Type: " + type.getTypeName());
		}
	}

	public static boolean equals(Type type1, Type type2) {
		boolean equals = false;
		if (type1 instanceof ParameterizedType && type2 instanceof ParameterizedType) {
			equals = equalsParameterizedType((ParameterizedType) type1, (ParameterizedType) type2);
		}
		if (type1 == type2) {
			equals = true;
		}

		if (equals == true)
			return equals;

		if (type1 == null || type2 == null) {
			equals = false;
		}
		if (equals == true)
			return equals;

		if (type1 instanceof Class<?> && type2 instanceof Class<?>) {
			equals = type1.equals(type2);
		}
		if (equals == true)
			return equals;

		if (equals == true)
			return equals;

		if (type1 instanceof GenericArrayType && type2 instanceof GenericArrayType) {
			equals = equals(((GenericArrayType) type1).getGenericComponentType(),
					((GenericArrayType) type2).getGenericComponentType());
		}
		if (equals == true)
			return equals;

		if (type1 instanceof Class<?> && ((Class<?>) type1).isArray() &&
				type2 instanceof Class<?> && ((Class<?>) type2).isArray()) {
			equals = equals(((Class<?>) type1).getComponentType(), ((Class<?>) type2).getComponentType());
		}
		if (equals == true)
			return equals;

		equals = getClassFromType(type1).isAssignableFrom(getClassFromType(type2));
		if (equals == true)
			return equals;

		equals = getClassFromType(type2).isAssignableFrom(getClassFromType(type1));
		if (equals == true)
			return equals;

		return equals;
	}

	private static boolean equalsParameterizedType(ParameterizedType type1, ParameterizedType type2) {
		if (!equals(type1.getRawType(), type2.getRawType())) {
			return false;
		}

		Type[] args1 = type1.getActualTypeArguments();
		Type[] args2 = type2.getActualTypeArguments();
		if (args1.length != args2.length) {
			return false;
		}

		for (int i = 0; i < args1.length; i++) {
			if (!equals(args1[i], args2[i])) {
				return false;
			}
		}
		return true;
	}

}
