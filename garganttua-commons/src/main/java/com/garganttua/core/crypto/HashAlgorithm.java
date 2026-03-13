package com.garganttua.core.crypto;

import lombok.Getter;

public enum HashAlgorithm {

	@Deprecated(forRemoval = true)
	MD5("MD5"),

	SHA_1("SHA-1"),
	SHA_224("SHA-224"),
	SHA_256("SHA-256"),
	SHA_384("SHA-384"),
	SHA_512("SHA-512"),
	SHA3_256("SHA3-256"),
	SHA3_384("SHA3-384"),
	SHA3_512("SHA3-512");

	@Getter
	private final String name;

	HashAlgorithm(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
