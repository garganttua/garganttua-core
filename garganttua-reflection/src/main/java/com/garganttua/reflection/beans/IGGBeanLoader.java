package com.garganttua.reflection.beans;

public interface IGGBeanLoader {

	<T> T getBean(String supplier, String name, String type, Class<T> clazz);

}
