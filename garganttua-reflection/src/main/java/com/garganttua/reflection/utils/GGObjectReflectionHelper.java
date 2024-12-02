package com.garganttua.reflection.utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.fields.GGFields;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGObjectReflectionHelper {

	public static Constructor<?> getConstructorWithNoParams(Class<?> classs) {
		try {
			return classs.getDeclaredConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

	public static Field getField(Class<?> objectClass, String fieldName) {
		for (Field f : objectClass.getDeclaredFields()) {
			if (f.getName().equals(fieldName)) {
				return f;
			}
		}
		if (objectClass.getSuperclass() != null) {
			return GGObjectReflectionHelper.getField(objectClass.getSuperclass(), fieldName);
		}
		return null;
	}

	public static Method getMethod(Class<?> objectClass, String methodName) {
		for (Method f : objectClass.getDeclaredMethods()) {
			if (f.getName().equals(methodName)) {
				return f;
			}
		}
		if (objectClass.getSuperclass() != null) {
			return getMethod(objectClass.getSuperclass(), methodName);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <destination> destination instanciateNewObject(Class<?> clazz) throws GGReflectionException {
		Constructor<?> ctor = GGObjectReflectionHelper.getConstructorWithNoParams(clazz);
		if (ctor != null) {
			try (GGConstructorAccessManager accessor = new GGConstructorAccessManager(ctor)) {
				return (destination) ctor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new GGReflectionException(e);
			}
		}
		throw new GGReflectionException("Class " + clazz.getSimpleName() + " does not have constructor with no params");
	}

	static public void setObjectFieldValue(Object entity, Field field, Object value) throws GGReflectionException {
		if (field == null) {
			throw new GGReflectionException(
					"Cannot set null field of object " + entity.getClass().getName() + " with value " + value);
		}

		try (GGFieldAccessManager manager = new GGFieldAccessManager(field)) {
			field.set(entity, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new GGReflectionException("Cannot set field " + field.getName() + " of object "
					+ entity.getClass().getName() + " with value " + value, e);
		}
	}

	public static Object getObjectFieldValue(Object entity, String fieldName) throws GGReflectionException {
		Field field = GGObjectReflectionHelper.getField(entity.getClass(), fieldName);
		if (field == null) {
			throw new GGReflectionException(
					"Cannot get field " + fieldName + " of object " + entity.getClass().getName());
		}

		return getObjectFieldValue(entity, field);
	}

	public static Object getObjectFieldValue(Object entity, Field field) throws GGReflectionException {
		try (GGFieldAccessManager manager = new GGFieldAccessManager(field)) {
			return field.get(entity);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new GGReflectionException(
					"Cannot get field " + field.getName() + " of object " + entity.getClass().getName(), e);
		}
	}

	public static Object invokeMethod(Object object, String methodName, Method method, Object... args)
			throws GGReflectionException {
		GGObjectReflectionHelper.checkMethodAndParams(method, args);

		try (GGMethodAccessManager manager = new GGMethodAccessManager(method)) {
			return method.invoke(object, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new GGReflectionException(
					"Cannot invoke method " + methodName + " of object " + object.getClass().getName(), e);
		}
	}

	public static void checkMethodAndParams(Method method, Object... args) throws GGReflectionException {
		if (method.getParameterCount() != args.length) {
			throw new GGReflectionException("Method " + method.getName() + " needs " + method.getParameterCount() + " "
					+ method.getParameterTypes() + " but " + args.length + " have been provided : " + args);
		}

		Class<?>[] params = method.getParameterTypes();
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				continue;
			if (!equals(params[i], args[i].getClass())) {
				throw new GGReflectionException("Method " + method.getName() + " needs parameter " + i
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

	public static List<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends Annotation> annotation)
			throws ClassNotFoundException, IOException {
		Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
		Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation, true);
		return annotatedClasses.stream().collect(Collectors.toList());
	}

	public static boolean isNotPrimitiveOrInternal(Class<?> clazz) {
		if (clazz.isPrimitive()) {
			return false;
		}
		Package package1 = clazz.getPackage();
		if (package1 == null) {
			return false;
		}

		String packageName = package1.getName();
		if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
			return false;
		}

		return true;
	}

	public static String getMethodAddressAnnotatedWithAndCheckMethodParamsHaveGoodTypes(Class<?> entityClass,
			Class<? extends Annotation> methodAnnotation, Type returnedType, Type ... methodParameters)
			throws GGReflectionException {
		String methodAddress = null;
		for (Method method : entityClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(methodAnnotation)) {
				if (methodAddress != null && !methodAddress.isEmpty()) {
					throw new GGReflectionException("Entity " + entityClass.getSimpleName()
							+ " has more than one method annotated with @" + methodAnnotation.getSimpleName());
				}
				methodAddress = method.getName();
				Type[] parameters = method.getGenericParameterTypes();

				if (parameters.length != methodParameters.length) {
					throw new GGReflectionException("Entity " + entityClass.getSimpleName() + " has method "
							+ methodAddress + " but wrong parameters number");
				}

				for (int i = 0; i < methodParameters.length; i++) {
					if (!equals(methodParameters[i], parameters[i])) {
						throw new GGReflectionException("Entity " + entityClass.getSimpleName() + " has method "
								+ methodAddress + " but parameter " + i + " is not of type "
								+ methodParameters[i].getTypeName());
					}
				}

				if (returnedType != null) {
					if (!method.getReturnType().isAssignableFrom(getClassFromType(returnedType))) {
						throw new GGReflectionException("Entity " + entityClass.getSimpleName() + " has method "
								+ methodAddress + " but returned type is not " + returnedType.getTypeName() + " but is "
								+ method.getGenericReturnType());
					}
				}
			}
		}

		if (methodAddress == null && entityClass.getSuperclass() != null) {
			methodAddress = getMethodAddressAnnotatedWithAndCheckMethodParamsHaveGoodTypes(entityClass.getSuperclass(),
					methodAnnotation, returnedType, methodParameters);
		}
		
		return methodAddress;
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
			Class<? extends Annotation> annotationClass, Type fieldType) throws GGReflectionException {
		log.atDebug().log("Looking for annotation " + annotationClass.getSimpleName() + " on field of type "
				+ getClassFromType(fieldType).getSimpleName() + " from type " + entityClass.getSimpleName());
		String fieldAddress = null;
		for (Field field : entityClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(annotationClass)) {
				if (fieldAddress != null && !fieldAddress.isEmpty()) {
					throw new GGReflectionException("Entity " + entityClass.getSimpleName()
							+ " has more than one field annotated with " + annotationClass);
				}
				if (equals(field.getGenericType(), fieldType)) {
					fieldAddress = field.getName();
					break;
				} else {
					throw new GGReflectionException(
							"Entity " + entityClass.getSimpleName() + " has field " + field.getName()
									+ " with wrong type " + field.getType().getName() + ", should be " + getClassFromType(fieldType));
				}
			} else {
				if (isNotPrimitiveOrInternal(field.getType()) && !entityClass.equals(field.getType())) {
					fieldAddress = getFieldAddressAnnotatedWithAndCheckType(field.getType(), annotationClass,
							fieldType);
					if (fieldAddress != null)
						break;
				}
			}
		}

		if (fieldAddress == null && entityClass.getSuperclass() != null) {
			fieldAddress = getFieldAddressAnnotatedWithAndCheckType(entityClass.getSuperclass(), annotationClass, fieldType);
		}

		return fieldAddress;
	}

	/**
	 * 
	 * @param entityClass
	 * @param annotation
	 * @param linkedAnnotation : check in field objects only if the annotation is found for field
	 * @return
	 */
	public static List<String> getFieldAddressesWithAnnotation(Class<?> entityClass, Class<? extends Annotation> annotation, boolean linkedAnnotation) {
		List<String> fieldsAddresses = new ArrayList<String>();
		getFieldAddressesWithAnnotation(fieldsAddresses, entityClass, annotation, linkedAnnotation);
		return fieldsAddresses; 
	}

	private static void getFieldAddressesWithAnnotation(List<String> fieldsAddresses, Class<?> entityClass,
			Class<? extends Annotation> annotation, boolean linkedAnnotation) {
		for( Field field: entityClass.getDeclaredFields()) {
			boolean found = false;
			if( field.isAnnotationPresent(annotation) ) {
				fieldsAddresses.add(field.getName());
				found = true;
			} 
			if ( ((found && linkedAnnotation) || !linkedAnnotation ) 
					&& isNotPrimitiveOrInternal(field.getType()) 
					&& !entityClass.equals(field.getType())) {
				getFieldAddressesWithAnnotation(fieldsAddresses, field.getType(), annotation, linkedAnnotation);
			}
			
			if ( ((found && linkedAnnotation) || !linkedAnnotation ) 
					&& GGFields.isArrayOrMapOrCollectionField(field) ) {
				int i = 0;
				
				Class<?> genericType = GGFields.getGenericType(field.getType(), i );
				while( genericType != null ) {
					getFieldAddressesWithAnnotation(fieldsAddresses, genericType, annotation, linkedAnnotation);
					i++;
					genericType = GGFields.getGenericType(field.getType(), i );
				};
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
	
	private static boolean equals(Type type1, Type type2) {
		boolean equals = false;
        if (type1 == type2) {
        	equals = true;
        }
        
        if( equals == true )
        	return equals;
        
        if (type1 == null || type2 == null) {
        	equals = false;
        }
        if( equals == true )
        	return equals;
        
        if (type1 instanceof Class<?> && type2 instanceof Class<?>) {
        	equals = type1.equals(type2);
        }
        if( equals == true )
        	return equals;

        if (type1 instanceof ParameterizedType && type2 instanceof ParameterizedType) {
        	equals = equalsParameterizedType((ParameterizedType) type1, (ParameterizedType) type2);
        }
        if( equals == true )
        	return equals;

        if (type1 instanceof GenericArrayType && type2 instanceof GenericArrayType) {
        	equals = equals(((GenericArrayType) type1).getGenericComponentType(),
                          ((GenericArrayType) type2).getGenericComponentType());
        }
        if( equals == true )
        	return equals;

        if (type1 instanceof Class<?> && ((Class<?>) type1).isArray() &&
            type2 instanceof Class<?> && ((Class<?>) type2).isArray()) {
        	equals = equals(((Class<?>) type1).getComponentType(), ((Class<?>) type2).getComponentType());
        }
        if( equals == true )
        	return equals;
        
        equals = getClassFromType(type1).isAssignableFrom(getClassFromType(type2));
        if( equals == true )
        	return equals;
        
        equals = getClassFromType(type2).isAssignableFrom(getClassFromType(type1));
        if( equals == true )
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
