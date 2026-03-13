package com.garganttua.core.crypto;

public interface IKeyAlgorithm {

	String getName();

	int getKeySize();

	KeyAlgorithmType getType();

	String getCipherName(EncryptionMode mode, EncryptionPaddingMode padding);

	String getSignatureName(SignatureAlgorithm signatureAlgorithm);

}
