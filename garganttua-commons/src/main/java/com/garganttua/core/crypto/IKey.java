package com.garganttua.core.crypto;

import java.security.Key;

public interface IKey {

	byte[] sign(byte[] data) throws CryptoException;

	boolean verifySignature(byte[] signature, byte[] originalData) throws CryptoException;

	byte[] encrypt(byte[] clear) throws CryptoException;

	byte[] decrypt(byte[] encoded) throws CryptoException;

	byte[] getRawKey();

	Key getKey() throws CryptoException;

	KeyType getType();

	IKeyAlgorithm getAlgorithm();

	EncryptionMode getEncryptionMode();

	EncryptionPaddingMode getEncryptionPaddingMode();

	SignatureAlgorithm getSignatureAlgorithm();

}
