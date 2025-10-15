package com.garganttua.executor.chain;

public class GGExecutorException extends Exception {

	private static final long serialVersionUID = 4089999852587836549L;

	public GGExecutorException(Throwable t) {
		super(t);
	}

	public GGExecutorException(String string) {
		super(string);
	}
}
