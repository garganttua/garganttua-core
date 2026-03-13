package com.garganttua.core.crypto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;

public class KeyRealmErrorTest {

	@Test
	public void testRevokedRealmThrowsOnGetKeyForEncryption() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("revoked-realm")
				.algorithm(KeyAlgorithm.AES_256)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		realm.revoke();
		assertTrue(realm.isRevoked());

		assertThrows(CryptoException.class, () -> realm.getKeyForEncryption());
		assertThrows(CryptoException.class, () -> realm.getKeyForDecryption());
		assertThrows(CryptoException.class, () -> realm.getKeyForSigning());
		assertThrows(CryptoException.class, () -> realm.getKeyForSignatureVerification());
	}

	@Test
	public void testExpiredRealmThrowsOnGetKey() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, -1);
		Date pastDate = cal.getTime();

		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("expired-realm")
				.algorithm(KeyAlgorithm.AES_256)
				.expiration(pastDate)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		assertTrue(realm.isExpired());
		assertThrows(CryptoException.class, () -> realm.getKeyForEncryption());
	}

	@Test
	public void testNonExpiredRealmWorks() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, 1);
		Date futureDate = cal.getTime();

		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("valid-realm")
				.algorithm(KeyAlgorithm.AES_256)
				.expiration(futureDate)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		assertFalse(realm.isExpired());
		assertDoesNotThrow(() -> realm.getKeyForEncryption());
	}

	@Test
	public void testSignWithNonPrivateKeyThrows() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("sign-error")
				.algorithm(KeyAlgorithm.RSA_2048)
				.signatureAlgorithm(SignatureAlgorithm.SHA256)
				.build();

		IKey publicKey = realm.getKeyForSignatureVerification();
		assertThrows(CryptoException.class, () -> publicKey.sign("data".getBytes()));
	}

	@Test
	public void testVerifyWithNonPublicKeyThrows() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("verify-error")
				.algorithm(KeyAlgorithm.RSA_2048)
				.signatureAlgorithm(SignatureAlgorithm.SHA256)
				.build();

		IKey privateKey = realm.getKeyForSigning();
		assertThrows(CryptoException.class, () -> privateKey.verifySignature("sig".getBytes(), "data".getBytes()));
	}

	@Test
	public void testDecryptCorruptedDataThrows() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("corrupt")
				.algorithm(KeyAlgorithm.AES_256)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		byte[] corruptedData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
		assertThrows(CryptoException.class, () -> realm.getKeyForDecryption().decrypt(corruptedData));
	}

	@Test
	public void testBuilderMissingNameThrows() {
		assertThrows(DslException.class, () -> KeyRealmBuilder.builder()
				.algorithm(KeyAlgorithm.AES_256)
				.build());
	}

	@Test
	public void testBuilderMissingAlgorithmThrows() {
		assertThrows(DslException.class, () -> KeyRealmBuilder.builder()
				.name("test")
				.build());
	}

	@Test
	public void testRotateCreatesNewVersion() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("rotate-test")
				.algorithm(KeyAlgorithm.AES_256)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		assertEquals(1, realm.getVersion());

		IKeyRealm rotated = realm.rotate();
		assertEquals(2, rotated.getVersion());
		assertEquals("rotate-test", rotated.getName());
		assertFalse(rotated.isRevoked());
	}

	@Test
	public void testRotatedRealmHasDifferentKeys() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("rotate-keys")
				.algorithm(KeyAlgorithm.AES_256)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		IKeyRealm rotated = realm.rotate();

		byte[] encrypted = realm.getKeyForEncryption().encrypt("test".getBytes());
		assertThrows(CryptoException.class, () -> rotated.getKeyForDecryption().decrypt(encrypted));
	}

	private static void assertEquals(Object expected, Object actual) {
		org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
	}

}
