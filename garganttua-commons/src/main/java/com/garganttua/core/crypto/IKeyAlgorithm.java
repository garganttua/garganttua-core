package com.garganttua.core.crypto;

/**
 * Describes a key algorithm and resolves the JCA transformation names for
 * the cipher and signature operations it supports.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IKeyAlgorithm {

	/**
	 * Returns the canonical algorithm name (e.g. {@code AES}, {@code RSA}).
	 *
	 * @return the algorithm name
	 */
	String getName();

	/**
	 * Returns the key size in bits.
	 *
	 * @return the key size in bits
	 */
	int getKeySize();

	/**
	 * Returns whether this algorithm is symmetric or asymmetric.
	 *
	 * @return the algorithm type
	 */
	KeyAlgorithmType getType();

	/**
	 * Resolves the JCA cipher transformation name for the given mode and padding.
	 *
	 * @param mode the block cipher mode
	 * @param padding the padding scheme
	 * @return the JCA transformation string (e.g. {@code AES/CBC/PKCS5Padding})
	 */
	String getCipherName(EncryptionMode mode, EncryptionPaddingMode padding);

	/**
	 * Resolves the JCA signature algorithm name for the given digest.
	 *
	 * @param signatureAlgorithm the digest algorithm to combine with this key algorithm
	 * @return the JCA signature algorithm name
	 */
	String getSignatureName(SignatureAlgorithm signatureAlgorithm);

}
