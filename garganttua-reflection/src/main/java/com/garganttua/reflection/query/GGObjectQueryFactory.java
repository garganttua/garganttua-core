package com.garganttua.reflection.query;

import com.garganttua.reflection.GGReflectionException;

public class GGObjectQueryFactory {
	
	public static IGGObjectQuery objectQuery(Class<?> objectClass) throws GGReflectionException {
		return new GGObjectQuery(objectClass);
	}
	
	public static IGGObjectQuery objectQuery(Object object) throws GGReflectionException {
		return new GGObjectQuery(object);
	}
	
	public static IGGObjectQuery objectQuery(Class<?> objectClass, Object object) throws GGReflectionException {
		return new GGObjectQuery(objectClass, object);
	}
}
