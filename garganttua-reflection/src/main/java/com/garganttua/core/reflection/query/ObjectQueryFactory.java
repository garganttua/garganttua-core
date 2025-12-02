package com.garganttua.core.reflection.query;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.IObjectQuery;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectQueryFactory {

	public static IObjectQuery objectQuery(Class<?> objectClass) throws ReflectionException {
		log.atDebug().log("Creating ObjectQuery for class: {}", objectClass);
		return new ObjectQuery(objectClass);
	}

	public static IObjectQuery objectQuery(Object object) throws ReflectionException {
		log.atDebug().log("Creating ObjectQuery for object: {}", object);
		return new ObjectQuery(object);
	}

	public static IObjectQuery objectQuery(Class<?> objectClass, Object object) throws ReflectionException {
		log.atDebug().log("Creating ObjectQuery for class: {} with object: {}", objectClass, object);
		return new ObjectQuery(objectClass, object);
	}
}
