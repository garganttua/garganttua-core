package com.garganttua.reflection.injection;

import com.garganttua.reflection.GGReflectionException;

public interface IGGInjector {

	void injectBeans(Object entity) throws GGReflectionException;

	void injectProperties(Object entity) throws GGReflectionException;

}
