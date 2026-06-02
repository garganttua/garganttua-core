package com.garganttua.core.crypto;

/**
 * The role a key plays within its algorithm.
 *
 * @since 2.0.0-ALPHA01
 */
public enum KeyType {

	/** Symmetric secret key used for both encryption and decryption. */
	SECRET,
	/** Private half of an asymmetric key pair (decryption, signing). */
	PRIVATE,
	/** Public half of an asymmetric key pair (encryption, verification). */
	PUBLIC

}
