package com.garganttua.core.reflection;

import com.garganttua.core.CoreException;

public class ReflectionException extends CoreException {

	public ReflectionException(String string) {
		super(CoreException.REFLECTION_ERROR, string);
	}

	public ReflectionException(String string, Throwable e) {
		super(CoreException.REFLECTION_ERROR, string, e);
	}

	public ReflectionException(Throwable e) {
		super(e);
	}

	private static final long serialVersionUID = 2732095843634378815L;

}
