package com.garganttua.core.crypto;

import java.util.Date;

public interface IKeyRealm {

	String getName();

	IKeyAlgorithm getKeyAlgorithm();

	IKey getKeyForDecryption() throws CryptoException;

	IKey getKeyForEncryption() throws CryptoException;

	IKey getKeyForSigning() throws CryptoException;

	IKey getKeyForSignatureVerification() throws CryptoException;

	void revoke();

	boolean isRevoked();

	Date getExpiration();

	boolean isExpired();

	int getVersion();

	IKeyRealm rotate();

}
