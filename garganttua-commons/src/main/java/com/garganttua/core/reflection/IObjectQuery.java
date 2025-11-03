package com.garganttua.core.reflection;

import java.util.List;

public interface IObjectQuery {

	List<Object> find(ObjectAddress address) throws ReflectionException;
	List<Object> find(String address) throws ReflectionException;
	
	ObjectAddress address(String elementName) throws ReflectionException;
	
	Object fieldValueStructure(ObjectAddress address) throws ReflectionException;
	Object fieldValueStructure(String address) throws ReflectionException;

	Object setValue(Object object, String fieldAddress, Object fieldValue) throws ReflectionException;
	Object setValue(Object object, ObjectAddress fieldAddress, Object fieldValue) throws ReflectionException;
	Object setValue(String fieldAddress, Object fieldValue) throws ReflectionException;
	Object setValue(ObjectAddress fieldAddress, Object fieldValue) throws ReflectionException;

	Object getValue(Object object, String fieldAddress) throws ReflectionException;
	Object getValue(Object object, ObjectAddress fieldAddress) throws ReflectionException;
	Object getValue(String fieldAddress) throws ReflectionException;
	Object getValue(ObjectAddress fieldAddress) throws ReflectionException;
	
	Object invoke(String methodAddress, Object ...args) throws ReflectionException;
	Object invoke(ObjectAddress methodAddress, Object ...args) throws ReflectionException;
	Object invoke(Object object, ObjectAddress methodAddress, Object ...args) throws ReflectionException;

}