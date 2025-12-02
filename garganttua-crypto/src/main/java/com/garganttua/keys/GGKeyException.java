package com.garganttua.keys;

import java.security.GeneralSecurityException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGKeyException extends Exception {

	public GGKeyException(Exception e) {
		super(e);
		log.atError().log("GGKeyException created with exception", e);
	}

	public GGKeyException(String string) {
		super(string);
		log.atError().log("GGKeyException created: {}", string);
	}

	public GGKeyException(String string, GeneralSecurityException e) {
		super(string, e);
		log.atError().log("GGKeyException created: {}", string, e);
	}

	private static final long serialVersionUID = -1273727827288538097L;

}
