package com.garganttua.core.crypto;

/**
 * Digest algorithms usable for signing and signature verification. Each
 * constant carries the canonical JCA-style name returned by {@link #getName()}.
 *
 * <p>{@link #MD2} and {@link #MD5} are retained only for legacy interoperability
 * and are deprecated for removal as they are cryptographically broken.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public enum SignatureAlgorithm {
	SHA1("SHA1"),
	SHA224("SHA224"),
	SHA256("SHA256"),
	SHA384("SHA384"),
	SHA512("SHA512"),
	SHA3_224("SHA3-224"),
	SHA3_256("SHA3-256"),
	SHA3_384("SHA3-384"),
	SHA3_512("SHA3-512"),

	@Deprecated(forRemoval = true)
	MD2("MD2"),
	@Deprecated(forRemoval = true)
	MD5("MD5"),

	RIPEMD128("RIPEMD128"),
	RIPEMD160("RIPEMD160"),
	RIPEMD256("RIPEMD256"),

	WHIRLPOOL("WHIRLPOOL"),

	GOST3411("GOST3411"),
	GOST3411_2012_256("GOST3411-2012-256"),
	GOST3411_2012_512("GOST3411-2012-512"),

	BLAKE2B_256("BLAKE2B-256"),
	BLAKE2B_384("BLAKE2B-384"),
	BLAKE2B_512("BLAKE2B-512"),
	BLAKE2S_256("BLAKE2S-256"),

	KECCAK_224("KECCAK-224"),
	KECCAK_256("KECCAK-256"),
	KECCAK_384("KECCAK-384"),
	KECCAK_512("KECCAK-512"),

	ED25519("Ed25519"),
	ED448("Ed448");

	private final String name;

	SignatureAlgorithm(String name) {
		this.name = name;
	}

	/**
	 * Returns the canonical algorithm name.
	 *
	 * @return the algorithm name
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
