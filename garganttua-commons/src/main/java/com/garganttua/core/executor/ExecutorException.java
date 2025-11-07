package com.garganttua.core.executor;

public class ExecutorException extends Exception {

	private static final long serialVersionUID = 4089999852587836549L;

	public ExecutorException(Throwable t) {
		super(t);
	}

	public ExecutorException(String string) {
		super(string);
	}
}
