package com.garganttua.core.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class KeySerializerTest {

	@Test
	public void testExportImportSymmetricKey() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("serialize-test")
				.algorithm(KeyAlgorithm.AES_256)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
				.build();

		IKey originalKey = realm.getKeyForEncryption();
		String exported = KeySerializer.exportRawKey(originalKey);
		assertNotNull(exported);

		IKey importedKey = KeySerializer.importRawKey(exported, KeyType.SECRET, KeyAlgorithm.AES_256,
				0, EncryptionMode.ECB, EncryptionPaddingMode.PKCS5_PADDING, null);

		byte[] encrypted = originalKey.encrypt("test data".getBytes());
		byte[] decrypted = importedKey.decrypt(encrypted);
		assertEquals("test data", new String(decrypted));
	}

	@Test
	public void testExportImportAsymmetricKeys() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("serialize-asym")
				.algorithm(KeyAlgorithm.RSA_2048)
				.encryptionMode(EncryptionMode.ECB)
				.paddingMode(EncryptionPaddingMode.PKCS1_PADDING)
				.build();

		IKey encKey = realm.getKeyForEncryption();
		IKey decKey = realm.getKeyForDecryption();

		String exportedPrivate = KeySerializer.exportRawKey(encKey);
		String exportedPublic = KeySerializer.exportRawKey(decKey);

		IKey importedPrivate = KeySerializer.importRawKey(exportedPrivate, KeyType.PRIVATE, KeyAlgorithm.RSA_2048,
				0, EncryptionMode.ECB, EncryptionPaddingMode.PKCS1_PADDING, null);
		IKey importedPublic = KeySerializer.importRawKey(exportedPublic, KeyType.PUBLIC, KeyAlgorithm.RSA_2048,
				0, EncryptionMode.ECB, EncryptionPaddingMode.PKCS1_PADDING, null);

		byte[] encrypted = importedPrivate.encrypt("hello".getBytes());
		byte[] decrypted = importedPublic.decrypt(encrypted);
		assertEquals("hello", new String(decrypted));
	}

}
