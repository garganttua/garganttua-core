package com.garganttua.core.execution;

import com.garganttua.core.CoreException;

public class ExecutorException extends CoreException {

	private static final long serialVersionUID = 4089999852587836549L;

	public ExecutorException(Exception t) {
		super(CoreException.EXECUTOR_ERROR, t);
	}

	public ExecutorException(String string) {
		super(CoreException.EXECUTOR_ERROR, string);
	}
	public ExecutorException(String string, Throwable t) {
		super(CoreException.EXECUTOR_ERROR, string, t);
	}
}
