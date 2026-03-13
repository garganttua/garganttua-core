package com.garganttua.core.crypto;

public interface IHash {

	byte[] hash(byte[] data) throws CryptoException;

	boolean verify(byte[] data, byte[] expectedHash) throws CryptoException;

	String getAlgorithm();

}
