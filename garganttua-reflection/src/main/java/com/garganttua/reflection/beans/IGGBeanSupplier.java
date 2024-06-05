package com.garganttua.reflection.beans;

public interface IGGBeanSupplier {

	<T> T getBean(String name, String type, Class<T> clazz);

	String getName();

}
