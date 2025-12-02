package com.garganttua.keys;

public interface IGGKeyRealm {

	String getName();
	
	GGKeyAlgorithm getKeyAlgorithm();
	
	boolean equals(IGGKeyRealm object);

	/**
	 * Actually returns a public key, or a secret key
	 * @return the key for decryption
	 * @throws GGKeyException if key retrieval fails
	 */
	IGGKey getKeyForDecryption() throws GGKeyException;

	/**
	 * Actually returns a private key, or a secret key
	 * @return the key for encryption
	 * @throws GGKeyException if key retrieval fails
	 */
	IGGKey getKeyForEncryption() throws GGKeyException;

	/**
	 * Returns a private key for signing
	 * @return the key for signing
	 * @throws GGKeyException if key retrieval fails
	 */
	IGGKey getKeyForSigning() throws GGKeyException;

	/**
	 * Returns a public key for signature verification
	 * @return the key for signature verification
	 * @throws GGKeyException if key retrieval fails
	 */
	IGGKey getKeyForSignatureVerification() throws GGKeyException;

	void revoke();

}
