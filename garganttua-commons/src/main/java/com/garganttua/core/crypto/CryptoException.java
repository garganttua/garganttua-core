package com.garganttua.core.crypto;

import com.garganttua.core.CoreException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CryptoException extends CoreException {

	private static final long serialVersionUID = 1L;

	public CryptoException(String message) {
		super(CoreException.CRYPTO_ERROR, message);
		log.atTrace().log("Exiting CryptoException constructor");
	}

	public CryptoException(String message, Exception cause) {
		super(CoreException.CRYPTO_ERROR, message, cause);
		log.atTrace().log("Exiting CryptoException constructor");
	}

	public CryptoException(Exception e) {
		super(CoreException.CRYPTO_ERROR, e);
		log.atTrace().log("Exiting CryptoException constructor");
	}

}
