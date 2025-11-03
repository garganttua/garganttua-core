package com.garganttua.core.reflection;

public class ReflectionException extends Exception {

	public ReflectionException(String string) {
		super(string);
	}

	public ReflectionException(String string, Exception e) {
		super(string, e);
	}

	public ReflectionException(Exception e) {
		super(e);
	}

	private static final long serialVersionUID = 2732095843634378815L;

}
