package com.garganttua.injection.spec.injection;

import com.garganttua.injection.DiException;

public interface IInjector {

	void doInjection(Object instance) throws DiException;

}
