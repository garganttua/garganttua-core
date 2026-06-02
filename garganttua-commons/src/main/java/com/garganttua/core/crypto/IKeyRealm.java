package com.garganttua.core.crypto;

import java.util.Date;

/**
 * A versioned collection of keys sharing a single algorithm, managing their
 * lifecycle (expiration, revocation, rotation) and dispensing the appropriate
 * key for each cryptographic operation.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IKeyRealm {

	/**
	 * Returns the realm name.
	 *
	 * @return the realm name
	 */
	String getName();

	/**
	 * Returns the algorithm shared by all keys in this realm.
	 *
	 * @return the key algorithm
	 */
	IKeyAlgorithm getKeyAlgorithm();

	/**
	 * Returns the key to use for decryption.
	 *
	 * @return the decryption key
	 * @throws CryptoException if no suitable key is available
	 */
	IKey getKeyForDecryption() throws CryptoException;

	/**
	 * Returns the key to use for encryption.
	 *
	 * @return the encryption key
	 * @throws CryptoException if no suitable key is available
	 */
	IKey getKeyForEncryption() throws CryptoException;

	/**
	 * Returns the key to use for signing.
	 *
	 * @return the signing key
	 * @throws CryptoException if no suitable key is available
	 */
	IKey getKeyForSigning() throws CryptoException;

	/**
	 * Returns the key to use for signature verification.
	 *
	 * @return the signature verification key
	 * @throws CryptoException if no suitable key is available
	 */
	IKey getKeyForSignatureVerification() throws CryptoException;

	/**
	 * Revokes this realm, marking its keys as no longer usable.
	 */
	void revoke();

	/**
	 * Returns whether this realm has been revoked.
	 *
	 * @return {@code true} if revoked
	 */
	boolean isRevoked();

	/**
	 * Returns the expiration date of this realm.
	 *
	 * @return the expiration date, or {@code null} if it never expires
	 */
	Date getExpiration();

	/**
	 * Returns whether this realm has passed its expiration date.
	 *
	 * @return {@code true} if expired
	 */
	boolean isExpired();

	/**
	 * Returns the version number of this realm.
	 *
	 * @return the version
	 */
	int getVersion();

	/**
	 * Produces a new realm with freshly generated keys and an incremented version,
	 * superseding this one.
	 *
	 * @return the rotated realm
	 */
	IKeyRealm rotate();

}
