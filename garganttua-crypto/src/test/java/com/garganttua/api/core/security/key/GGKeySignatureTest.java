package com.garganttua.api.core.security.key;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.keys.GGKeyAlgorithm;
import com.garganttua.keys.GGKeyException;
import com.garganttua.keys.GGKeyRealm;
import com.garganttua.keys.GGSignatureAlgorithm;
import com.garganttua.keys.IGGKey;

public class GGKeySignatureTest {
	
	@Test
	public void testSignatureSHA224withRSA() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.RSA_4096, null, GGSignatureAlgorithm.SHA224);
		
		IGGKey signingKey = realm.getKeyForSigning();
		IGGKey verifiingKey = realm.getKeyForSignatureVerification();
		
		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifiingKey.verifySignature(signature, "Salut".getBytes());
		
		assertTrue(signatureOk);
	}
	
	@Test
	public void testSignatureSHA256withECDSA() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.EC_256, null, GGSignatureAlgorithm.SHA256);
		
		IGGKey signingKey = realm.getKeyForSigning();
		IGGKey verifiingKey = realm.getKeyForSignatureVerification();
		
		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifiingKey.verifySignature(signature, "Salut".getBytes());
		
		assertTrue(signatureOk);
	}
	
	@Test
	public void testSignatureSHA1withRSA() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.RSA_4096, null, GGSignatureAlgorithm.SHA1);
		
		IGGKey signingKey = realm.getKeyForSigning();
		IGGKey verifiingKey = realm.getKeyForSignatureVerification();
		
		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifiingKey.verifySignature(signature, "Salut".getBytes());
		
		assertTrue(signatureOk);
	}
	
	@Test
	public void testSignatureSHA256withDSA() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.DSA_2048, null, GGSignatureAlgorithm.SHA256);
		
		IGGKey signingKey = realm.getKeyForSigning();
		IGGKey verifiingKey = realm.getKeyForSignatureVerification();
		
		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifiingKey.verifySignature(signature, "Salut".getBytes());
		
		assertTrue(signatureOk);
	}
	
	@Test
	public void testSignatureMD5withRSA() throws GGKeyException {
		GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.RSA_3072, null, GGSignatureAlgorithm.MD5);
		
		IGGKey signingKey = realm.getKeyForSigning();
		IGGKey verifiingKey = realm.getKeyForSignatureVerification();
		
		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifiingKey.verifySignature(signature, "Salut".getBytes());
		
		assertTrue(signatureOk);
	}

}
