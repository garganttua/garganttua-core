package com.garganttua.api.core.security.key;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.garganttua.keys.GGEncryptionMode;
import com.garganttua.keys.GGEncryptionPaddingMode;
import com.garganttua.keys.GGKeyAlgorithm;
import com.garganttua.keys.GGKeyException;
import com.garganttua.keys.GGKeyRealm;

public class GGKeyRealmEncryptDecryptTest {
	
	@Test
	public void testEncryptDecryptRSA4096_ECB_PKCS1_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.RSA_4096, null, GGEncryptionMode.ECB, GGEncryptionPaddingMode.PKCS1_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecryptAES256_ECB_PKCS5_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, GGEncryptionMode.ECB, GGEncryptionPaddingMode.PKCS5_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecryptAES256_CBC_PKCS5_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 16, GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecryptAES256_GCM_NO_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 12, GGEncryptionMode.GCM, GGEncryptionPaddingMode.NO_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecryptAES256_CTR_NO_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 16, GGEncryptionMode.CTR, GGEncryptionPaddingMode.NO_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecryptAES256_CFB_NO_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 16, GGEncryptionMode.CFB, GGEncryptionPaddingMode.NO_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
//	@Test
	public void testEncryptDecryptRSA512_NONE_NO_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.RSA_512, GGEncryptionMode.NONE, GGEncryptionPaddingMode.NO_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
//	@Test
	public void testEncryptDecryptEC384_ECDSA_NO_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.EC_384, GGEncryptionMode.ECDSA, GGEncryptionPaddingMode.NONE);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
//	@Test
	public void testEncryptDecryptDH1024_NONE_NO_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.DH_1024, GGEncryptionMode.NONE, GGEncryptionPaddingMode.NONE);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecrypt3DES168_CBC_PKCS1_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.DESEDE_168, 8, GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecryptDES56_CBC_PKCS1_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.DES_56, 8, GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}
	
	@Test
	public void testEncryptDecryptBLOWFISH120_CBC_PKCS5_PADDING() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.BLOWFISH_120, 8, GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);
		
		byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
		byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
		
		byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
		byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
		
		assertEquals("salut", new String(decryptWithPrivate));
		assertEquals("salut", new String(decryptWithPublic));
	}

}
