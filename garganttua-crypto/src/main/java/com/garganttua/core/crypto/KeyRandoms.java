package com.garganttua.core.crypto;

import com.garganttua.core.observability.Logger;
import java.security.SecureRandom;

/**
 * Holder for the framework-wide {@link SecureRandom} instance, eagerly created and
 * self-seeded at class load. All key generation and IV creation in this package
 * draws from {@link #secureRandom()}.
 */
public class KeyRandoms {
    private static final Logger log = Logger.getLogger(KeyRandoms.class);

	private static final SecureRandom DEFAULT_SECURE_RANDOM;

	static {
		log.debug("Initializing SecureRandom instance");
		DEFAULT_SECURE_RANDOM = new SecureRandom();
		DEFAULT_SECURE_RANDOM.nextBytes(new byte[64]);
		log.debug("SecureRandom initialized and seeded");
	}

	private KeyRandoms() {
	}

	/** {@return the shared, seeded {@link SecureRandom} instance} */
	public static SecureRandom secureRandom() {
		log.trace("Retrieving SecureRandom instance");
		return DEFAULT_SECURE_RANDOM;
	}
}
