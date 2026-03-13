package com.garganttua.core.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class KeyRealmEncryptDecryptTest {

	@Test
	public void testEncryptDecryptRSA4096_ECB_PKCS1_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.RSA_4096)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS1_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecryptAES256_ECB_PKCS5_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.AES_256)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecryptAES256_CBC_PKCS5_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.AES_256)
				.initializationVectorSize(16)
				.encryptionMode(EncryptionMode.CBC)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecryptAES256_GCM_NO_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.AES_256)
				.initializationVectorSize(12)
				.encryptionMode(EncryptionMode.GCM)
				.paddingMode(EncryptionPaddingMode.NO_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecryptAES256_CTR_NO_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.AES_256)
				.initializationVectorSize(16)
				.encryptionMode(EncryptionMode.CTR)
				.paddingMode(EncryptionPaddingMode.NO_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecryptAES256_CFB_NO_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.AES_256)
				.initializationVectorSize(16)
				.encryptionMode(EncryptionMode.CFB)
				.paddingMode(EncryptionPaddingMode.NO_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecrypt3DES168_CBC_PKCS5_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.DESEDE_168)
				.initializationVectorSize(8)
				.encryptionMode(EncryptionMode.CBC)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecryptDES56_CBC_PKCS5_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.DES_56)
				.initializationVectorSize(8)
				.encryptionMode(EncryptionMode.CBC)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

	@Test
	public void testEncryptDecryptBLOWFISH120_CBC_PKCS5_PADDING() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.BLOWFISH_120)
				.initializationVectorSize(8)
				.encryptionMode(EncryptionMode.CBC)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

}
