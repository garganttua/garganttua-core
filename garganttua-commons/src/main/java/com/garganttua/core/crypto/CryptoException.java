package com.garganttua.core.crypto;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.CoreException;

/**
 * Exception raised by cryptographic operations (hashing, encryption, key
 * handling). Carries the {@link CoreException#CRYPTO_ERROR} code.
 *
 * @since 2.0.0-ALPHA01
 * @see CoreException
 */
public class CryptoException extends CoreException {
    private static final Logger log = Logger.getLogger(CryptoException.class);

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new exception with the given detail message.
	 *
	 * @param message description of the cryptographic failure
	 */
	public CryptoException(String message) {
		super(CoreException.CRYPTO_ERROR, message);
		log.trace("Exiting CryptoException constructor");
	}

	/**
	 * Constructs a new exception with the given detail message and cause.
	 *
	 * @param message description of the cryptographic failure
	 * @param cause   the underlying cause
	 */
	public CryptoException(String message, Exception cause) {
		super(CoreException.CRYPTO_ERROR, message, cause);
		log.trace("Exiting CryptoException constructor");
	}

	/**
	 * Constructs a new exception wrapping the given cause.
	 *
	 * @param e the underlying cause
	 */
	public CryptoException(Exception e) {
		super(CoreException.CRYPTO_ERROR, e);
		log.trace("Exiting CryptoException constructor");
	}

}
