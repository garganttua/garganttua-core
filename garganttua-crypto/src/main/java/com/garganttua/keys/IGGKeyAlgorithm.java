package com.garganttua.keys;

import java.security.KeyPair;

import javax.crypto.SecretKey;

public interface IGGKeyAlgorithm {

	GGKeyRealmType getType() throws IllegalArgumentException;

	SecretKey generateSymetricKey() throws IllegalArgumentException;

	KeyPair generateAsymetricKey() throws IllegalArgumentException;

	String geCipherName(GGEncryptionMode mode, GGEncryptionPaddingMode padding) throws IllegalArgumentException;

	String geSignatureName(GGSignatureAlgorithm signatureAlgorithm);

}
