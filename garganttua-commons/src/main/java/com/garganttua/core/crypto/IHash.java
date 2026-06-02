package com.garganttua.core.crypto;

/**
 * Computes and verifies cryptographic digests of arbitrary byte data.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IHash {

	/**
	 * Computes the digest of the supplied data.
	 *
	 * @param data the bytes to hash
	 * @return the computed hash
	 * @throws CryptoException if the digest cannot be computed
	 */
	byte[] hash(byte[] data) throws CryptoException;

	/**
	 * Verifies that the digest of {@code data} matches {@code expectedHash}.
	 *
	 * @param data the bytes to hash and compare
	 * @param expectedHash the previously computed hash to compare against
	 * @return {@code true} if the recomputed digest equals {@code expectedHash}
	 * @throws CryptoException if the digest cannot be computed
	 */
	boolean verify(byte[] data, byte[] expectedHash) throws CryptoException;

	/**
	 * Returns the canonical name of the underlying hash algorithm.
	 *
	 * @return the algorithm name
	 */
	String getAlgorithm();

}
