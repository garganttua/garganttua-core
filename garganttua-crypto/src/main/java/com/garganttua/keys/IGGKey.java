package com.garganttua.keys;

import java.security.Key;

public interface IGGKey {
	
	byte[] sign(byte[] data) throws GGKeyException;

	boolean verifySignature(byte[] signature, byte[] originalData) throws GGKeyException;
	
	byte[] encrypt(byte[] clear) throws GGKeyException;
	
	byte[] decrypt(byte[] encoded) throws GGKeyException;
	
	/**
	 * Base64 encoded key
	 * @return
	 */
	byte[] getRawKey();

	Key getKey() throws GGKeyException;

	GGKeyType getType();

	GGKeyAlgorithm getAlgorithm();

	byte[] getInitializationVector();

	GGEncryptionMode getEncryptionMode();

	GGEncryptionPaddingMode getEncryptionPaddingMode();

	GGSignatureAlgorithm getSignatureAlgorithm();

}
