package com.garganttua.core.crypto;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.security.SecureRandom;

public class KeyRandoms {
    private static final IDiagnostic log = Diagnostics.of(KeyRandoms.class);

	private static final SecureRandom DEFAULT_SECURE_RANDOM;

	static {
		log.debug("Initializing SecureRandom instance");
		DEFAULT_SECURE_RANDOM = new SecureRandom();
		DEFAULT_SECURE_RANDOM.nextBytes(new byte[64]);
		log.debug("SecureRandom initialized and seeded");
	}

	private KeyRandoms() {
	}

	public static SecureRandom secureRandom() {
		log.trace("Retrieving SecureRandom instance");
		return DEFAULT_SECURE_RANDOM;
	}
}
