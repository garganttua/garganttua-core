package com.garganttua.core.crypto;

/**
 * Supported message-digest algorithms, each carrying its standard JCA algorithm
 * name.
 *
 * @since 2.0.0-ALPHA01
 */
public enum HashAlgorithm {

	/**
	 * MD5. Cryptographically broken; retained only for legacy interop.
	 *
	 * @deprecated insecure; use a SHA-2 or SHA-3 variant instead
	 */
	@Deprecated(forRemoval = true)
	MD5("MD5"),

	/** SHA-1 ({@code SHA-1}). */
	SHA_1("SHA-1"),
	/** SHA-2, 224-bit ({@code SHA-224}). */
	SHA_224("SHA-224"),
	/** SHA-2, 256-bit ({@code SHA-256}). */
	SHA_256("SHA-256"),
	/** SHA-2, 384-bit ({@code SHA-384}). */
	SHA_384("SHA-384"),
	/** SHA-2, 512-bit ({@code SHA-512}). */
	SHA_512("SHA-512"),
	/** SHA-3, 256-bit ({@code SHA3-256}). */
	SHA3_256("SHA3-256"),
	/** SHA-3, 384-bit ({@code SHA3-384}). */
	SHA3_384("SHA3-384"),
	/** SHA-3, 512-bit ({@code SHA3-512}). */
	SHA3_512("SHA3-512");

	private final String name;

	HashAlgorithm(String name) {
		this.name = name;
	}

	/**
	 * @return the standard JCA algorithm name (e.g. {@code "SHA-256"})
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
