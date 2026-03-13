package com.garganttua.core.crypto;

import java.util.Date;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;

public class KeyRealmBuilder implements IBuilder<IKeyRealm> {

	private String name;
	private KeyAlgorithm keyAlgorithm;
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

	public KeyRealmBuilder name(String name) {
		this.name = name;
		return this;
	}

	public KeyRealmBuilder algorithm(KeyAlgorithm keyAlgorithm) {
		this.keyAlgorithm = keyAlgorithm;
		return this;
	}

	public KeyRealmBuilder expiration(Date expiration) {
		this.expiration = expiration;
		return this;
	}

	public KeyRealmBuilder initializationVectorSize(int size) {
		this.initializationVectorSize = size;
		return this;
	}

	public KeyRealmBuilder encryptionMode(EncryptionMode mode) {
		this.encryptionMode = mode;
		return this;
	}

	public KeyRealmBuilder paddingMode(EncryptionPaddingMode paddingMode) {
		this.paddingMode = paddingMode;
		return this;
	}

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
