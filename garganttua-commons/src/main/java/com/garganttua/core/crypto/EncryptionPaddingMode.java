package com.garganttua.core.crypto;

import lombok.Getter;

public enum EncryptionPaddingMode {

	NO_PADDING("NoPadding"),
	PKCS5_PADDING("PKCS5Padding"),
	ISO10126_PADDING("ISO10126Padding"),
	PKCS7_PADDING("PKCS7Padding"),
	PKCS1_PADDING("PKCS1Padding"),
	NONE("None");

	@Getter
	private final String padding;

	EncryptionPaddingMode(String padding) {
		this.padding = padding;
	}

}
