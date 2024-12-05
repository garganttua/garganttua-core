package com.garganttua.reflection.fields;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGFields {
	
	static public Class<?> getGenericType(Field field, int genericTypeIndex) {
		return getGenericType(field.getGenericType(), genericTypeIndex);
	}
	
	static public Class<?> getGenericType(Class<?> clazz, int genericTypeIndex) {
		return getGenericType(clazz.getGenericSuperclass(), genericTypeIndex);
	}

	private static Class<?> getGenericType(Type type, int genericTypeIndex) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > 0 && typeArguments[genericTypeIndex] instanceof Class<?>) {
				return (Class<?>) typeArguments[genericTypeIndex];
			}
		}
		return null;
	}
	
	public static boolean isNotPrimitive(Class<?> clazz) {
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
		if( !isNotPrimitive(clazz) ) {
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
	
	public static boolean isArrayOrMapOrCollectionField(Field field) {
		return Collection.class.isAssignableFrom(field.getType()) || 
				Map.class.isAssignableFrom(field.getType()) || 
				field.getType().isArray();
	}

	public static Object instanciate(Field field) throws GGReflectionException {
		if( log.isDebugEnabled() ) {
			log.debug("Instanciating Field Object of type {}", field.getType().getSimpleName());
		}
		Object object = null;
		
		try {
			object =  GGObjectReflectionHelper.instanciateNewObject(field.getType());
		} catch (IllegalArgumentException | SecurityException | GGReflectionException e) {
			log.warn("Exception during instanciation : {}, triing instanciating supported interface object", e.getMessage());
			return GGFields.instanciatePrimitiveOrInterfaceObjectOr(field);
		}

		return object;	
	}

	private static Object instanciatePrimitiveOrInterfaceObjectOr(Field field) throws GGReflectionException {
		if (field.getType() == int.class ) {
			return (int) 1;
		}
		if (field.getType() == long.class ) {
			return (long) 0L;
		}
		if (field.getType() == float.class ) {
			return (float) 0F;
		}
		if (field.getType() == double.class ) {
			return (double) 0D;
		}
		if (field.getType() == short.class ) {
			return (short) 0;
		}
		if (field.getType() == byte.class ) {
			return (byte) 0x00;
		}
		if (field.getType() == char.class ) {
			return (char) '0';
		}
		if (field.getType() == boolean.class ) {
			return (boolean) false;
		}
		if( field.getType().isArray() ) {
			return Array.newInstance(field.getType().getComponentType(), 0);
		}
		if( Map.class.isAssignableFrom(field.getType()) ) {
			return GGObjectReflectionHelper.newHashMapOf(GGFields.getGenericType(field, 0), GGFields.getGenericType(field, 1));
		}
		if( List.class.isAssignableFrom(field.getType()) ) {
			return GGObjectReflectionHelper.newArrayListOf(GGFields.getGenericType(field, 0));
		}
		if( Set.class.isAssignableFrom(field.getType()) ) {
			return GGObjectReflectionHelper.newHashSetOf(GGFields.getGenericType(field, 0));
		}
		if( Queue.class.isAssignableFrom(field.getType()) ) {
			return GGObjectReflectionHelper.newLinkedlistOf(GGFields.getGenericType(field, 0));
		}
		if( Collection.class.isAssignableFrom(field.getType()) ) {
			return GGObjectReflectionHelper.newVectorOf(GGFields.getGenericType(field, 0));
		}
		throw new GGReflectionException("Unable to instanciate object of type "+field.getType().getSimpleName());
	}
}
