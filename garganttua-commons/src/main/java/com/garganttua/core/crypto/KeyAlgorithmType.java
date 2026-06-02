package com.garganttua.core.crypto;

/**
 * Classifies a key algorithm by its key structure.
 *
 * @since 2.0.0-ALPHA01
 */
public enum KeyAlgorithmType {
	/** Single shared key for both operations (e.g. AES). */
	SYMMETRIC,
	/** Distinct public/private key pair (e.g. RSA, EC). */
	ASYMMETRIC
}
