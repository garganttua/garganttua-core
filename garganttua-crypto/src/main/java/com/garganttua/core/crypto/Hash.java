package com.garganttua.core.crypto;

import com.garganttua.core.observability.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * {@link IHash} implementation backed by a JCA {@link MessageDigest}. Each call
 * resolves a fresh digest for the configured {@link HashAlgorithm}, so instances
 * are stateless and safe to share.
 */
public class Hash implements IHash {
    private static final Logger log = Logger.getLogger(Hash.class);

	private final HashAlgorithm algorithm;

	/**
	 * Creates a hasher for the given algorithm.
	 *
	 * @param algorithm the digest algorithm to use
	 */
	public Hash(HashAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * Computes the digest of {@code data}.
	 *
	 * @param data the bytes to hash
	 * @return the digest bytes
	 * @throws CryptoException if the algorithm is unavailable in the JCA provider
	 */
	@Override
	public byte[] hash(byte[] data) throws CryptoException {
		log.debug("Hashing with algorithm={}", algorithm.getName());
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm.getName());
			return digest.digest(data);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Hash error", e);
		}
	}

	/**
	 * Hashes {@code data} and compares the result against {@code expectedHash}
	 * using a constant-time comparison.
	 *
	 * @param data         the bytes to hash
	 * @param expectedHash the digest to compare against
	 * @return {@code true} if the computed digest matches {@code expectedHash}
	 * @throws CryptoException if the algorithm is unavailable in the JCA provider
	 */
	@Override
	public boolean verify(byte[] data, byte[] expectedHash) throws CryptoException {
		byte[] actualHash = hash(data);
		return MessageDigest.isEqual(actualHash, expectedHash);
	}

	/** {@return the JCA name of the configured hash algorithm} */
	@Override
	public String getAlgorithm() {
		return algorithm.getName();
	}

}
