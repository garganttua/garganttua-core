package com.garganttua.core.reflection.runtime;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

public class RuntimeReflectionProvider implements IReflectionProvider {

	@Override
	public <T> IClass<T> getClass(Class<T> clazz) {
		return RuntimeClass.of(clazz);
	}

	@Override
	public IClass<?> forName(String className) throws ClassNotFoundException {
		return RuntimeClass.ofUnchecked(Class.forName(className));
	}

	@Override
	public IClass<?> forName(String className, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		return RuntimeClass.ofUnchecked(Class.forName(className, initialize, loader));
	}

	@Override
	public boolean supports(Class<?> type) {
		return true;
	}
}
