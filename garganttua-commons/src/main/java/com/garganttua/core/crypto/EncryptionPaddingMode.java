package com.garganttua.core.crypto;

/**
 * Cipher padding schemes, each carrying the JCE padding name used to build a
 * transformation string.
 *
 * @since 2.0.0-ALPHA01
 */
public enum EncryptionPaddingMode {

	/** No padding ({@code NoPadding}). */
	NO_PADDING("NoPadding"),
	/** PKCS#5 padding ({@code PKCS5Padding}). */
	PKCS5_PADDING("PKCS5Padding"),
	/** ISO 10126 padding ({@code ISO10126Padding}). */
	ISO10126_PADDING("ISO10126Padding"),
	/** PKCS#7 padding ({@code PKCS7Padding}). */
	PKCS7_PADDING("PKCS7Padding"),
	/** PKCS#1 padding ({@code PKCS1Padding}). */
	PKCS1_PADDING("PKCS1Padding"),
	/** No padding ({@code None}). */
	NONE("None");

	private final String padding;

	EncryptionPaddingMode(String padding) {
		this.padding = padding;
	}

	/**
	 * @return the JCE padding name (e.g. {@code "PKCS5Padding"})
	 */
	public String getPadding() {
		return padding;
	}

}
