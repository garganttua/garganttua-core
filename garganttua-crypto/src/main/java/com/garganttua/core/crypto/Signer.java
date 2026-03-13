package com.garganttua.core.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class Signer {

	static byte[] sign(PrivateKey key, String signatureAlgorithm, byte[] data) throws CryptoException {
		log.atDebug().log("Signing with algorithm={}", signatureAlgorithm);
		try {
			Signature signature = Signature.getInstance(signatureAlgorithm);
			signature.initSign(key);
			signature.update(data);
			return signature.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new CryptoException("Signature error", e);
		}
	}

	static boolean verify(PublicKey key, String signatureAlgorithm, byte[] signature, byte[] data) throws CryptoException {
		log.atDebug().log("Verifying signature with algorithm={}", signatureAlgorithm);
		try {
			Signature signatureVerify = Signature.getInstance(signatureAlgorithm);
			signatureVerify.initVerify(key);
			signatureVerify.update(data);
			return signatureVerify.verify(signature);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new CryptoException("Signature verification error", e);
		}
	}

}
