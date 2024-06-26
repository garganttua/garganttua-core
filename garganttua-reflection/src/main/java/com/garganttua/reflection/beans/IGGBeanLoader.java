package com.garganttua.reflection.beans;

import java.util.List;

import com.garganttua.reflection.GGReflectionException;

public interface IGGBeanLoader {

	Object getBeanNamed(String supplier, String name) throws GGReflectionException;
	
	<T> T getBeanOfType(String supplier, Class<T> type) throws GGReflectionException;
	
	<T> List<T> getBeansImplementingInterface(String supplier, Class<T> interfasse) throws GGReflectionException;

}
