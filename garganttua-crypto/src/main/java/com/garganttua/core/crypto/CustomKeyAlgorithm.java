package com.garganttua.core.crypto;

import lombok.Getter;

public class CustomKeyAlgorithm implements IKeyAlgorithm {

	@Getter
	private final String name;

	@Getter
	private final int keySize;

	@Getter
	private final KeyAlgorithmType type;

	public CustomKeyAlgorithm(String name, int keySize, KeyAlgorithmType type) {
		this.name = name;
		this.keySize = keySize;
		this.type = type;
	}

	@Override
	public String getCipherName(EncryptionMode mode, EncryptionPaddingMode padding) {
		if (mode == null || padding == null) {
			throw new IllegalArgumentException("Mode and Padding cannot be null");
		}
		return name + "/" + mode + "/" + padding.getPadding();
	}

	@Override
	public String getSignatureName(SignatureAlgorithm signatureAlgorithm) {
		if (signatureAlgorithm == null) {
			throw new IllegalArgumentException("Signature algorithm cannot be null");
		}
		return signatureAlgorithm.getName() + "with" + name;
	}

	@Override
	public String toString() {
		return name + "_" + keySize;
	}

}
