package com.garganttua.core.crypto;

import java.security.SecureRandom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeyRandoms {
	private static final SecureRandom DEFAULT_SECURE_RANDOM;

	static {
		log.atDebug().log("Initializing SecureRandom instance");
		DEFAULT_SECURE_RANDOM = new SecureRandom();
		DEFAULT_SECURE_RANDOM.nextBytes(new byte[64]);
		log.atDebug().log("SecureRandom initialized and seeded");
	}

	private KeyRandoms() {
	}

	public static SecureRandom secureRandom() {
		log.atTrace().log("Retrieving SecureRandom instance");
		return DEFAULT_SECURE_RANDOM;
	}
}
