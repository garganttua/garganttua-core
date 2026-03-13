package com.garganttua.core.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class KeyGenerators {

	static SecretKey generateSymmetricKey(IKeyAlgorithm algorithm) {
		if (algorithm instanceof KeyAlgorithm ka) {
			return ka.generateSymmetricKey();
		}
		log.atDebug().log("Generating symmetric key for custom algorithm: {}, size: {}", algorithm.getName(), algorithm.getKeySize());
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance(algorithm.getName());
			keyGen.init(algorithm.getKeySize(), KeyRandoms.secureRandom());
			return keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Unsupported algorithm: " + algorithm.getName(), e);
		}
	}

	static KeyPair generateAsymmetricKey(IKeyAlgorithm algorithm) {
		if (algorithm instanceof KeyAlgorithm ka) {
			return ka.generateAsymmetricKey();
		}
		log.atDebug().log("Generating asymmetric key for custom algorithm: {}, size: {}", algorithm.getName(), algorithm.getKeySize());
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.getName());
			if (algorithm.getName().equals("EC")) {
				String curveName = algorithm.getKeySize() == 512 ? "secp521r1" : "secp" + algorithm.getKeySize() + "r1";
				keyGen.initialize(new ECGenParameterSpec(curveName), KeyRandoms.secureRandom());
			} else {
				keyGen.initialize(algorithm.getKeySize(), KeyRandoms.secureRandom());
			}
			return keyGen.generateKeyPair();
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new IllegalArgumentException("Unsupported algorithm: " + algorithm.getName(), e);
		}
	}

	private KeyGenerators() {
	}

}
