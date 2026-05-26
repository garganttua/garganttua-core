package com.garganttua.core.crypto;

public class CustomKeyAlgorithm implements IKeyAlgorithm {

	private final String name;

	private final int keySize;

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
	public String getName() {
		return this.name;
	}

	@Override
	public int getKeySize() {
		return this.keySize;
	}

	@Override
	public KeyAlgorithmType getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return name + "_" + keySize;
	}

}
