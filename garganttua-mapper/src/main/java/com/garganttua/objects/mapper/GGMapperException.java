package com.garganttua.objects.mapper;

public class GGMapperException extends Exception {

	private static final long serialVersionUID = 3629256996026750672L;
	
	public GGMapperException(String string) {
		super(string);
	}

	public GGMapperException(String string, Exception e) {
		super(string, e);
	}

	public GGMapperException(Exception e) {
		super(e);
	}

}
