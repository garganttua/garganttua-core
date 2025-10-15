package com.garganttua.keys;

import java.security.SecureRandom;

public class GGKeyRandoms {
	private static final SecureRandom DEFAULT_SECURE_RANDOM;

    static {
        DEFAULT_SECURE_RANDOM = new SecureRandom();
        DEFAULT_SECURE_RANDOM.nextBytes(new byte[64]);
    }

    private GGKeyRandoms() {
    }

    public static SecureRandom secureRandom() {
        return DEFAULT_SECURE_RANDOM;
    }
}