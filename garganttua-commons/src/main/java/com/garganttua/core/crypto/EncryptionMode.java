package com.garganttua.core.crypto;

/**
 * Block-cipher modes of operation used when configuring encryption.
 *
 * @since 2.0.0-ALPHA01
 */
public enum EncryptionMode {
	/** Electronic Code Book. */
	ECB,
	/** Cipher Block Chaining. */
	CBC,
	/** Cipher Feedback. */
	CFB,
	/** Output Feedback. */
	OFB,
	/** Galois/Counter Mode (authenticated encryption). */
	GCM,
	/** Counter mode. */
	CTR,
	/** No mode of operation. */
	NONE,
	/** Elliptic Curve Digital Signature Algorithm. */
	ECDSA
}
