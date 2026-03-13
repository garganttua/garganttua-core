package com.garganttua.core.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HashTest {

	@Test
	public void testSHA256Hash() throws CryptoException {
		IHash hash = new Hash(HashAlgorithm.SHA_256);
		byte[] data = "Hello World".getBytes();
		byte[] digest = hash.hash(data);

		assertNotNull(digest);
		assertEquals(32, digest.length);
	}

	@Test
	public void testSHA512Hash() throws CryptoException {
		IHash hash = new Hash(HashAlgorithm.SHA_512);
		byte[] data = "Hello World".getBytes();
		byte[] digest = hash.hash(data);

		assertNotNull(digest);
		assertEquals(64, digest.length);
	}

	@Test
	public void testSHA256Deterministic() throws CryptoException {
		IHash hash = new Hash(HashAlgorithm.SHA_256);
		byte[] data = "test data".getBytes();

		byte[] hash1 = hash.hash(data);
		byte[] hash2 = hash.hash(data);

		assertArrayEquals(hash1, hash2);
	}

	@Test
	public void testVerifyCorrectHash() throws CryptoException {
		IHash hash = new Hash(HashAlgorithm.SHA_256);
		byte[] data = "verify me".getBytes();
		byte[] digest = hash.hash(data);

		assertTrue(hash.verify(data, digest));
	}

	@Test
	public void testVerifyIncorrectHash() throws CryptoException {
		IHash hash = new Hash(HashAlgorithm.SHA_256);
		byte[] data = "verify me".getBytes();
		byte[] wrongHash = new byte[32];

		assertFalse(hash.verify(data, wrongHash));
	}

	@Test
	public void testDifferentDataProducesDifferentHash() throws CryptoException {
		IHash hash = new Hash(HashAlgorithm.SHA_256);
		byte[] hash1 = hash.hash("data1".getBytes());
		byte[] hash2 = hash.hash("data2".getBytes());

		assertFalse(java.util.Arrays.equals(hash1, hash2));
	}

	@Test
	public void testGetAlgorithm() {
		IHash hash = new Hash(HashAlgorithm.SHA_256);
		assertEquals("SHA-256", hash.getAlgorithm());
	}

	@Test
	public void testSHA3_256() throws CryptoException {
		IHash hash = new Hash(HashAlgorithm.SHA3_256);
		byte[] digest = hash.hash("test".getBytes());
		assertNotNull(digest);
		assertEquals(32, digest.length);
	}

	private static void assertEquals(Object expected, Object actual) {
		org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
	}

}
