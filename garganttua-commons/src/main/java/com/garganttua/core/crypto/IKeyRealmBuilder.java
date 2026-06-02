package com.garganttua.core.crypto;

import java.util.Date;

import com.garganttua.core.dsl.IBuilder;

/**
 * Fluent builder for {@link IKeyRealm} instances.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IKeyRealmBuilder extends IBuilder<IKeyRealm> {

	/**
	 * Sets the realm name.
	 *
	 * @param name the realm name
	 * @return this builder for method chaining
	 */
	IKeyRealmBuilder name(String name);

	/**
	 * Sets the algorithm shared by the realm's keys.
	 *
	 * @param algorithm the key algorithm
	 * @return this builder for method chaining
	 */
	IKeyRealmBuilder algorithm(IKeyAlgorithm algorithm);

	/**
	 * Sets the realm expiration date.
	 *
	 * @param expiration the expiration date
	 * @return this builder for method chaining
	 */
	IKeyRealmBuilder expiration(Date expiration);

	/**
	 * Sets the initialization vector size in bytes used for encryption.
	 *
	 * @param size the IV size in bytes
	 * @return this builder for method chaining
	 */
	IKeyRealmBuilder initializationVectorSize(int size);

	/**
	 * Sets the block cipher mode used for encryption.
	 *
	 * @param mode the encryption mode
	 * @return this builder for method chaining
	 */
	IKeyRealmBuilder encryptionMode(EncryptionMode mode);

	/**
	 * Sets the padding scheme used for encryption.
	 *
	 * @param paddingMode the padding mode
	 * @return this builder for method chaining
	 */
	IKeyRealmBuilder paddingMode(EncryptionPaddingMode paddingMode);

	/**
	 * Sets the digest algorithm used for signing.
	 *
	 * @param signatureAlgorithm the signature algorithm
	 * @return this builder for method chaining
	 */
	IKeyRealmBuilder signatureAlgorithm(SignatureAlgorithm signatureAlgorithm);

}
