package com.garganttua.core.crypto;

public interface IKeyRealm {

	String getName();

	IKeyAlgorithm getKeyAlgorithm();

	IKey getKeyForDecryption() throws CryptoException;

	IKey getKeyForEncryption() throws CryptoException;

	IKey getKeyForSigning() throws CryptoException;

	IKey getKeyForSignatureVerification() throws CryptoException;

	void revoke();

}
