package com.garganttua.core.crypto;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.CoreException;

public class CryptoException extends CoreException {
    private static final IDiagnostic log = Diagnostics.of(CryptoException.class);

	private static final long serialVersionUID = 1L;

	public CryptoException(String message) {
		super(CoreException.CRYPTO_ERROR, message);
		log.trace("Exiting CryptoException constructor");
	}

	public CryptoException(String message, Exception cause) {
		super(CoreException.CRYPTO_ERROR, message, cause);
		log.trace("Exiting CryptoException constructor");
	}

	public CryptoException(Exception e) {
		super(CoreException.CRYPTO_ERROR, e);
		log.trace("Exiting CryptoException constructor");
	}

}
