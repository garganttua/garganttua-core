package com.garganttua.keys;

public interface IGGKeyRealm {

	String getName();
	
	GGKeyAlgorithm getKeyAlgorithm();
	
	boolean equals(IGGKeyRealm object);

	/**
	 * Actually returns a public key, or a secret key
	 * @return
	 * @throws GGAPIException
	 */
	IGGKey getKeyForDecryption() throws GGKeyException;

	/**
	 * Actually returns a private key, or a secret key
	 * @return
	 * @throws GGAPIException
	 */
	IGGKey getKeyForEncryption() throws GGKeyException;
	
	/**
	 * Returns a private key for signing
	 * @return
	 * @throws GGAPIException
	 */
	IGGKey getKeyForSigning() throws GGKeyException;
	
	/**
	 * Returns a public key for signature verification
	 * @return
	 * @throws GGAPIException
	 */
	IGGKey getKeyForSignatureVerification() throws GGKeyException;

	void revoke();

}
