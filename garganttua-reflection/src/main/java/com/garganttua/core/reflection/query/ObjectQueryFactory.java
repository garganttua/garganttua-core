package com.garganttua.core.reflection.query;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.IObjectQuery;

public class ObjectQueryFactory {
	
	public static IObjectQuery objectQuery(Class<?> objectClass) throws ReflectionException {
		return new ObjectQuery(objectClass);
	}
	
	public static IObjectQuery objectQuery(Object object) throws ReflectionException {
		return new ObjectQuery(object);
	}
	
	public static IObjectQuery objectQuery(Class<?> objectClass, Object object) throws ReflectionException {
		return new ObjectQuery(objectClass, object);
	}
}
