package com.garganttua.reflection;

public class GGReflectionException extends Exception {

	public GGReflectionException(String string) {
		super(string);
	}

	public GGReflectionException(String string, Exception e) {
		super(string, e);
	}

	public GGReflectionException(Exception e) {
		super(e);
	}

	private static final long serialVersionUID = 2732095843634378815L;

}
