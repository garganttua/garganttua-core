package com.garganttua.reflection.beans;

import java.util.List;

import com.garganttua.reflection.GGReflectionException;

public interface IGGBeanSupplier {

	Object getBeanNamed(String name) throws GGReflectionException;
	
	<T> T getBeanOfType(Class<T> type) throws GGReflectionException;
	
	<T> List<T> getBeansImplementingInterface(Class<T> interfasse) throws GGReflectionException;  

	String getName();

}
