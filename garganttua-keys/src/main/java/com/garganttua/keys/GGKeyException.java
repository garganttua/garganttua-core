package com.garganttua.keys;

import java.security.GeneralSecurityException;

public class GGKeyException extends Exception {

	public GGKeyException(Exception e) {
		super(e);
	}

	public GGKeyException(String string) {
		super(string);
	}

	public GGKeyException(String string, GeneralSecurityException e) {
		super(string, e);
	}

	private static final long serialVersionUID = -1273727827288538097L;

}
