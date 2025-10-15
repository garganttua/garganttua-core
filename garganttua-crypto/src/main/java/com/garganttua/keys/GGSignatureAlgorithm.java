package com.garganttua.keys;

public enum GGSignatureAlgorithm {
	// SHA-based signature algorithms
    SHA1("SHA1"),
    SHA224("SHA224"),
    SHA256("SHA256"),
    SHA384("SHA384"),
    SHA512("SHA512"),
    SHA3_224("SHA3-224"),
    SHA3_256("SHA3-256"),
    SHA3_384("SHA3-384"),
    SHA3_512("SHA3-512"),
    
    // HmacSHA1
//    HMAC_SHA1("HmacSHA1"),
//
//    // HmacSHA224
//    HMAC_SHA224("HmacSHA224"),
//
//    // HmacSHA256
//    HMAC_SHA256("HmacSHA256"),
//
//    // HmacSHA384
//    HMAC_SHA384("HmacSHA384"),
//
//    // HmacSHA512
//    HMAC_SHA512_128("HmacSHA512"),


    // MD-based signature algorithms
    MD2("MD2"),
    MD5("MD5"),

    // RIPEMD-based signature algorithms
    RIPEMD128("RIPEMD128"),
    RIPEMD160("RIPEMD160"),
    RIPEMD256("RIPEMD256"),

    // Whirlpool-based signature algorithms
    WHIRLPOOL("WHIRLPOOL"),

    // GOST signature algorithms
    GOST3411("GOST3411"),
    GOST3411_2012_256("GOST3411-2012-256"),
    GOST3411_2012_512("GOST3411-2012-512"),

    // Blake-based signature algorithms
    BLAKE2B_256("BLAKE2B-256"),
    BLAKE2B_384("BLAKE2B-384"),
    BLAKE2B_512("BLAKE2B-512"),
    BLAKE2S_256("BLAKE2S-256"),

    // Keccak-based signature algorithms
    KECCAK_224("KECCAK-224"),
    KECCAK_256("KECCAK-256"),
    KECCAK_384("KECCAK-384"),
    KECCAK_512("KECCAK-512"),

    // Other cryptographic signature algorithms
    ED25519("Ed25519"),
    ED448("Ed448");

    private final String name;

    GGSignatureAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
