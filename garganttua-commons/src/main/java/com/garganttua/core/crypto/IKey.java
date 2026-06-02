package com.garganttua.core.crypto;

import java.security.Key;

/**
 * A cryptographic key bound to an algorithm, exposing the signing, verification,
 * encryption, and decryption operations it supports.
 *
 * <p>The set of operations actually available depends on the key {@link KeyType}
 * and {@link IKeyAlgorithm}; unsupported operations raise {@link CryptoException}.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IKey {

	/**
	 * Signs the given data with this key.
	 *
	 * @param data the bytes to sign
	 * @return the signature
	 * @throws CryptoException if this key cannot sign or signing fails
	 */
	byte[] sign(byte[] data) throws CryptoException;

	/**
	 * Verifies a signature against the original data using this key.
	 *
	 * @param signature the signature to check
	 * @param originalData the data that was signed
	 * @return {@code true} if the signature is valid for {@code originalData}
	 * @throws CryptoException if verification cannot be performed
	 */
	boolean verifySignature(byte[] signature, byte[] originalData) throws CryptoException;

	/**
	 * Encrypts cleartext with this key.
	 *
	 * @param clear the plaintext bytes
	 * @return the ciphertext
	 * @throws CryptoException if this key cannot encrypt or encryption fails
	 */
	byte[] encrypt(byte[] clear) throws CryptoException;

	/**
	 * Decrypts ciphertext with this key.
	 *
	 * @param encoded the ciphertext bytes
	 * @return the recovered plaintext
	 * @throws CryptoException if this key cannot decrypt or decryption fails
	 */
	byte[] decrypt(byte[] encoded) throws CryptoException;

	/**
	 * Returns the raw key material as bytes.
	 *
	 * @return the encoded key bytes
	 */
	byte[] getRawKey();

	/**
	 * Returns the underlying JCA {@link Key}.
	 *
	 * @return the JCA key
	 * @throws CryptoException if the key cannot be materialized
	 */
	Key getKey() throws CryptoException;

	/**
	 * Returns the role of this key (secret, private, or public).
	 *
	 * @return the key type
	 */
	KeyType getType();

	/**
	 * Returns the algorithm this key is bound to.
	 *
	 * @return the key algorithm
	 */
	IKeyAlgorithm getAlgorithm();

	/**
	 * Returns the block cipher mode used for encryption operations.
	 *
	 * @return the encryption mode
	 */
	EncryptionMode getEncryptionMode();

	/**
	 * Returns the padding scheme used for encryption operations.
	 *
	 * @return the encryption padding mode
	 */
	EncryptionPaddingMode getEncryptionPaddingMode();

	/**
	 * Returns the digest algorithm used for signing operations.
	 *
	 * @return the signature algorithm
	 */
	SignatureAlgorithm getSignatureAlgorithm();

}
