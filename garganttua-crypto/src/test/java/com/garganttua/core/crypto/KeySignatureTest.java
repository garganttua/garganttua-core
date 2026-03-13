package com.garganttua.core.crypto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class KeySignatureTest {

	@Test
	public void testSignatureSHA224withRSA() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.RSA_4096)
				.signatureAlgorithm(SignatureAlgorithm.SHA224)
				.build();

		IKey signingKey = realm.getKeyForSigning();
		IKey verifyingKey = realm.getKeyForSignatureVerification();

		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());

		assertTrue(signatureOk);
	}

	@Test
	public void testSignatureSHA256withECDSA() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.EC_256)
				.signatureAlgorithm(SignatureAlgorithm.SHA256)
				.build();

		IKey signingKey = realm.getKeyForSigning();
		IKey verifyingKey = realm.getKeyForSignatureVerification();

		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());

		assertTrue(signatureOk);
	}

	@Test
	public void testSignatureSHA1withRSA() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.RSA_4096)
				.signatureAlgorithm(SignatureAlgorithm.SHA1)
				.build();

		IKey signingKey = realm.getKeyForSigning();
		IKey verifyingKey = realm.getKeyForSignatureVerification();

		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());

		assertTrue(signatureOk);
	}

	@Test
	public void testSignatureSHA256withDSA() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.DSA_2048)
				.signatureAlgorithm(SignatureAlgorithm.SHA256)
				.build();

		IKey signingKey = realm.getKeyForSigning();
		IKey verifyingKey = realm.getKeyForSignatureVerification();

		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());

		assertTrue(signatureOk);
	}

	@Test
	public void testSignatureMD5withRSA() throws Exception {
		IKeyRealm realm = KeyRealmBuilder.builder()
				.name("toto")
				.algorithm(KeyAlgorithm.RSA_3072)
				.signatureAlgorithm(SignatureAlgorithm.MD5)
				.build();

		IKey signingKey = realm.getKeyForSigning();
		IKey verifyingKey = realm.getKeyForSignatureVerification();

		byte[] signature = signingKey.sign("Salut".getBytes());
		boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());

		assertTrue(signatureOk);
	}

}
