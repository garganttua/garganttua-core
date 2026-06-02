package com.garganttua.core.crypto;

import java.util.Date;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder for {@link KeyRealm}. Configure an algorithm plus either an
 * encryption profile (mode/padding/IV size) or a signature profile, then call
 * {@link #build()}. The {@link #forEncryption} and {@link #forSignature} factory
 * methods provide pre-seeded starting points.
 */
@Reflected
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

	/** {@return a new, empty builder} */
	public static KeyRealmBuilder builder() {
		return new KeyRealmBuilder();
	}

	/**
	 * {@return a builder pre-seeded for encryption with the given algorithm, mode and padding}
	 *
	 * @param algorithm the key algorithm
	 * @param mode      the encryption mode
	 * @param padding   the padding scheme
	 */
	public static KeyRealmBuilder forEncryption(IKeyAlgorithm algorithm, EncryptionMode mode, EncryptionPaddingMode padding) {
		return builder().algorithm(algorithm).encryptionMode(mode).paddingMode(padding);
	}

	/**
	 * {@return a builder pre-seeded for signing with the given algorithm and signature algorithm}
	 *
	 * @param algorithm          the key algorithm
	 * @param signatureAlgorithm the signature digest algorithm
	 */
	public static KeyRealmBuilder forSignature(IKeyAlgorithm algorithm, SignatureAlgorithm signatureAlgorithm) {
		return builder().algorithm(algorithm).signatureAlgorithm(signatureAlgorithm);
	}

	/**
	 * Sets the realm identifier (required).
	 *
	 * @param name the realm name
	 * @return this builder
	 */
	@Override
	public KeyRealmBuilder name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the key algorithm (required).
	 *
	 * @param keyAlgorithm the algorithm to generate keys for
	 * @return this builder
	 */
	@Override
	public KeyRealmBuilder algorithm(IKeyAlgorithm keyAlgorithm) {
		this.keyAlgorithm = keyAlgorithm;
		return this;
	}

	/**
	 * Sets the absolute expiration date.
	 *
	 * @param expiration the expiration date, or {@code null} for no expiry
	 * @return this builder
	 */
	@Override
	public KeyRealmBuilder expiration(Date expiration) {
		this.expiration = expiration;
		return this;
	}

	/**
	 * Sets the initialization-vector size in bytes.
	 *
	 * @param size the IV size, or a non-positive value to disable IV usage
	 * @return this builder
	 */
	@Override
	public KeyRealmBuilder initializationVectorSize(int size) {
		this.initializationVectorSize = size;
		return this;
	}

	/**
	 * Sets the encryption mode.
	 *
	 * @param mode the block-cipher mode of operation
	 * @return this builder
	 */
	@Override
	public KeyRealmBuilder encryptionMode(EncryptionMode mode) {
		this.encryptionMode = mode;
		return this;
	}

	/**
	 * Sets the padding scheme.
	 *
	 * @param paddingMode the padding scheme
	 * @return this builder
	 */
	@Override
	public KeyRealmBuilder paddingMode(EncryptionPaddingMode paddingMode) {
		this.paddingMode = paddingMode;
		return this;
	}

	/**
	 * Sets the signature digest algorithm.
	 *
	 * @param signatureAlgorithm the signature algorithm
	 * @return this builder
	 */
	@Override
	public KeyRealmBuilder signatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
		this.signatureAlgorithm = signatureAlgorithm;
		return this;
	}

	/**
	 * Builds the configured {@link KeyRealm}, generating fresh keys.
	 *
	 * @return the built realm
	 * @throws DslException if the name or algorithm is missing
	 */
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
