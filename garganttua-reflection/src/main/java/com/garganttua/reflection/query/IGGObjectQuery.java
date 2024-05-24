package com.garganttua.reflection.query;

import java.util.List;

import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;

public interface IGGObjectQuery {

	List<Object> find(GGObjectAddress address) throws GGReflectionException;
	List<Object> find(String address) throws GGReflectionException;
	
	GGObjectAddress address(String elementName) throws GGReflectionException;
	
	Object fieldValueStructure(GGObjectAddress address) throws GGReflectionException;
	Object fieldValueStructure(String address) throws GGReflectionException;

	Object setValue(Object object, String fieldAddress, Object fieldValue) throws GGReflectionException;
	Object setValue(Object object, GGObjectAddress fieldAddress, Object fieldValue) throws GGReflectionException;
	Object setValue(String fieldAddress, Object fieldValue) throws GGReflectionException;
	Object setValue(GGObjectAddress fieldAddress, Object fieldValue) throws GGReflectionException;

	Object getValue(Object object, String fieldAddress) throws GGReflectionException;
	Object getValue(Object object, GGObjectAddress fieldAddress) throws GGReflectionException;
	Object getValue(String fieldAddress) throws GGReflectionException;
	Object getValue(GGObjectAddress fieldAddress) throws GGReflectionException;
	
	Object invoke(String methodAddress, Object ...args) throws GGReflectionException;
	Object invoke(GGObjectAddress methodAddress, Object ...args) throws GGReflectionException;
	Object invoke(Object object, GGObjectAddress methodAddress, Object ...args) throws GGReflectionException;

}