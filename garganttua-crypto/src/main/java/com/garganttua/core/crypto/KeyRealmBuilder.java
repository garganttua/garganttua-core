package com.garganttua.core.crypto;

import java.util.Date;

import com.garganttua.core.dsl.DslException;

public class KeyRealmBuilder implements IKeyRealmBuilder {

	private String name;
	private IKeyAlgorithm keyAlgorithm;
	private Date expiration;
	private int initializationVectorSize = -1;
	private EncryptionMode encryptionMode;
	private EncryptionPaddingMode paddingMode;
	private SignatureAlgorithm signatureAlgorithm;

	private KeyRealmBuilder() {
	}

	public static KeyRealmBuilder builder() {
		return new KeyRealmBuilder();
	}

	public static KeyRealmBuilder forEncryption(IKeyAlgorithm algorithm, EncryptionMode mode, EncryptionPaddingMode padding) {
		return builder().algorithm(algorithm).encryptionMode(mode).paddingMode(padding);
	}

	public static KeyRealmBuilder forSignature(IKeyAlgorithm algorithm, SignatureAlgorithm signatureAlgorithm) {
		return builder().algorithm(algorithm).signatureAlgorithm(signatureAlgorithm);
	}

	@Override
	public KeyRealmBuilder name(String name) {
		this.name = name;
		return this;
	}

	@Override
	public KeyRealmBuilder algorithm(IKeyAlgorithm keyAlgorithm) {
		this.keyAlgorithm = keyAlgorithm;
		return this;
	}

	@Override
	public KeyRealmBuilder expiration(Date expiration) {
		this.expiration = expiration;
		return this;
	}

	@Override
	public KeyRealmBuilder initializationVectorSize(int size) {
		this.initializationVectorSize = size;
		return this;
	}

	@Override
	public KeyRealmBuilder encryptionMode(EncryptionMode mode) {
		this.encryptionMode = mode;
		return this;
	}

	@Override
	public KeyRealmBuilder paddingMode(EncryptionPaddingMode paddingMode) {
		this.paddingMode = paddingMode;
		return this;
	}

	@Override
	public KeyRealmBuilder signatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
		this.signatureAlgorithm = signatureAlgorithm;
		return this;
	}

	@Override
	public IKeyRealm build() throws DslException {
		if (this.name == null || this.name.isBlank()) {
			throw new DslException("KeyRealm name is required");
		}
		if (this.keyAlgorithm == null) {
			throw new DslException("KeyRealm algorithm is required");
		}
		return new KeyRealm(this.name, this.keyAlgorithm, this.expiration, this.initializationVectorSize,
				this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
	}

}
