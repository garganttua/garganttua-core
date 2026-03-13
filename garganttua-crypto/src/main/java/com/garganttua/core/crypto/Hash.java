package com.garganttua.core.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Hash implements IHash {

	private final HashAlgorithm algorithm;

	public Hash(HashAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	@Override
	public byte[] hash(byte[] data) throws CryptoException {
		log.atDebug().log("Hashing with algorithm={}", algorithm.getName());
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
