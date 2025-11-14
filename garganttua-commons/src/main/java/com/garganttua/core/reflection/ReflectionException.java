package com.garganttua.core.reflection;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class ReflectionException extends CoreException {

	public ReflectionException(String string) {
		super(CoreExceptionCode.REFLECTION_ERROR, string);
	}

	public ReflectionException(String string, Exception e) {
		super(CoreExceptionCode.REFLECTION_ERROR, string, e);
	}

	public ReflectionException(Exception e) {
		super(e);
	}

	private static final long serialVersionUID = 2732095843634378815L;

}
