package com.garganttua.core.execution;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class ExecutorException extends CoreException {

	private static final long serialVersionUID = 4089999852587836549L;

	public ExecutorException(Exception t) {
		super(CoreExceptionCode.EXECUTOR_ERROR, t);
	}

	public ExecutorException(String string) {
		super(CoreExceptionCode.EXECUTOR_ERROR, string);
	}
}
