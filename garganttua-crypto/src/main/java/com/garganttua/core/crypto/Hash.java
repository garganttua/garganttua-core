package com.garganttua.core.crypto;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash implements IHash {
    private static final IDiagnostic log = Diagnostics.of(Hash.class);

	private final HashAlgorithm algorithm;

	public Hash(HashAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

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

	@Override
	public boolean verify(byte[] data, byte[] expectedHash) throws CryptoException {
		byte[] actualHash = hash(data);
		return MessageDigest.isEqual(actualHash, expectedHash);
	}

	@Override
	public String getAlgorithm() {
		return algorithm.getName();
	}

}
