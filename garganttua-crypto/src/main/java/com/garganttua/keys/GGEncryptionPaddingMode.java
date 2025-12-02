package com.garganttua.keys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum GGEncryptionPaddingMode {

	NO_PADDING("NoPadding"),
    PKCS5_PADDING("PKCS5Padding"),
    ISO10126_PADDING("ISO10126Padding"),
    PKCS7_PADDING("PKCS7Padding"),
	PKCS1_PADDING("PKCS1Padding"),
	NONE("None");

    private final String padding;

    GGEncryptionPaddingMode(String padding) {
        this.padding = padding;
    }

    public String getPadding() {
        log.atTrace().log("Getting padding mode: {}", this.padding);
        return padding;
    }

}
