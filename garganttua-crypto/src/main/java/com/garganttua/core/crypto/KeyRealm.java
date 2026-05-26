package com.garganttua.core.crypto;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.security.KeyPair;
import java.util.Date;
import java.util.Objects;

import javax.crypto.SecretKey;

import lombok.Getter;

public class KeyRealm implements IKeyRealm {
    private static final IDiagnostic log = Diagnostics.of(KeyRealm.class);

	@Getter
	protected String name;

	protected int ivSize;

	protected EncryptionMode encryptionMode;

	protected EncryptionPaddingMode paddingMode;

	protected SignatureAlgorithm signatureAlgorithm;

	@Getter
	protected IKeyAlgorithm keyAlgorithm;

	@Getter
	protected KeyAlgorithmType type;

	protected Key encryptionKey;

	protected Key decryptionKey;

	@Getter
	protected Date expiration;

	@Getter
	protected boolean revoked;

	@Getter
	protected int version = 1;

	KeyRealm(String name, IKeyAlgorithm keyAlgorithm, Date expiration, int initializationVectorSize,
			EncryptionMode encryptionMode, EncryptionPaddingMode paddingMode, SignatureAlgorithm signatureAlgorithm) {
		log.debug("Creating KeyRealm name={}, algorithm={}, ivSize={}, mode={}", name, keyAlgorithm, initializationVectorSize, encryptionMode);
		this.name = name;
		this.keyAlgorithm = keyAlgorithm;
		this.expiration = expiration;
		this.ivSize = initializationVectorSize > 0 ? initializationVectorSize : 0;
		this.encryptionMode = encryptionMode;
		this.paddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		if (keyAlgorithm != null) {
			this.type = keyAlgorithm.getType();
			this.createKeys();
		}
		log.debug("KeyRealm initialized name={}, type={}", this.name, this.type);
	}

	/**
	 * Private ctor used by the material-based factories. Skips
	 * {@link #createKeys()} (no JDK key generation), wrapping the supplied
	 * {@link Key}s instead. The realm is otherwise identical to one produced by
	 * {@code KeyRealmBuilder.build()}.
	 */
	private KeyRealm(String name, IKeyAlgorithm keyAlgorithm, Date expiration, int initializationVectorSize,
			EncryptionMode encryptionMode, EncryptionPaddingMode paddingMode, SignatureAlgorithm signatureAlgorithm,
			Key encryptionKey, Key decryptionKey, boolean revoked) {
		this.name = name;
		this.keyAlgorithm = keyAlgorithm;
		this.expiration = expiration;
		this.ivSize = initializationVectorSize > 0 ? initializationVectorSize : 0;
		this.encryptionMode = encryptionMode;
		this.paddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		this.type = keyAlgorithm != null ? keyAlgorithm.getType() : null;
		this.encryptionKey = encryptionKey;
		this.decryptionKey = decryptionKey;
		this.revoked = revoked;
		log.debug("KeyRealm reconstructed name={}, type={}, revoked={}",
				this.name, this.type, this.revoked);
	}

	/**
	 * Reconstruct a signing realm from persisted key material — typical use
	 * case is loading a previously-generated asymmetric key pair from a
	 * database row. The returned realm does <strong>not</strong> regenerate;
	 * it wraps the provided bytes.
	 *
	 * @param name               realm identifier (e.g. {@code "users:global"})
	 * @param algorithm          asymmetric algorithm of the persisted pair
	 * @param signatureAlgorithm signature algorithm for sign/verify
	 * @param expiration         absolute expiration date, or {@code null}
	 * @param revoked            revocation flag at load time
	 * @param privateBytes       PKCS#8 of the private key (signing)
	 * @param publicBytes        X.509 of the public key (verification)
	 * @return a {@link KeyRealm} wrapping the persisted material
	 * @since 2.0.0-ALPHA02
	 */
	public static KeyRealm fromSignatureMaterial(String name,
			IKeyAlgorithm algorithm, SignatureAlgorithm signatureAlgorithm,
			Date expiration, boolean revoked,
			byte[] privateBytes, byte[] publicBytes) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(algorithm, "algorithm");
		Objects.requireNonNull(signatureAlgorithm, "signatureAlgorithm");
		Objects.requireNonNull(privateBytes, "privateBytes");
		Objects.requireNonNull(publicBytes, "publicBytes");
		Key priv = Key.fromSigningMaterial(KeyType.PRIVATE, algorithm, signatureAlgorithm, privateBytes);
		Key pub = Key.fromSigningMaterial(KeyType.PUBLIC, algorithm, signatureAlgorithm, publicBytes);
		return new KeyRealm(name, algorithm, expiration, 0, null, null, signatureAlgorithm, priv, pub, revoked);
	}

	/**
	 * Reconstruct an encryption realm from persisted key material. For
	 * symmetric algorithms, {@code privateBytes} is the secret and
	 * {@code publicBytes} is ignored (callers may pass the same array or
	 * {@code null}). For asymmetric algorithms, both pairs of bytes are used.
	 *
	 * @param name        realm identifier
	 * @param algorithm   algorithm of the persisted material
	 * @param mode        encryption mode (block-cipher mode of operation)
	 * @param padding     padding scheme
	 * @param ivSize      IV size in bytes (0 if not applicable)
	 * @param expiration  absolute expiration date, or {@code null}
	 * @param revoked     revocation flag at load time
	 * @param privateBytes  PKCS#8 (asymmetric) or raw secret (symmetric)
	 * @param publicBytes   X.509 (asymmetric) or {@code null} (symmetric)
	 * @return a {@link KeyRealm} wrapping the persisted material
	 * @since 2.0.0-ALPHA02
	 */
	public static KeyRealm fromEncryptionMaterial(String name,
			IKeyAlgorithm algorithm, EncryptionMode mode, EncryptionPaddingMode padding,
			int ivSize, Date expiration, boolean revoked,
			byte[] privateBytes, byte[] publicBytes) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(algorithm, "algorithm");
		Objects.requireNonNull(mode, "mode");
		Objects.requireNonNull(padding, "padding");
		Objects.requireNonNull(privateBytes, "privateBytes");
		Key enc;
		Key dec;
		if (algorithm.getType() == KeyAlgorithmType.SYMMETRIC) {
			enc = Key.fromEncryptionMaterial(KeyType.SECRET, algorithm, mode, padding, ivSize, privateBytes);
			dec = Key.fromEncryptionMaterial(KeyType.SECRET, algorithm, mode, padding, ivSize, privateBytes);
		} else {
			Objects.requireNonNull(publicBytes, "publicBytes (required for asymmetric algorithm)");
			enc = Key.fromEncryptionMaterial(KeyType.PRIVATE, algorithm, mode, padding, ivSize, privateBytes);
			dec = Key.fromEncryptionMaterial(KeyType.PUBLIC, algorithm, mode, padding, ivSize, publicBytes);
		}
		return new KeyRealm(name, algorithm, expiration, ivSize, mode, padding, null, enc, dec, revoked);
	}

	private void createKeys() {
		if (this.type == KeyAlgorithmType.SYMMETRIC) {
			SecretKey key = KeyGenerators.generateSymmetricKey(this.keyAlgorithm);
			this.encryptionKey = new Key(KeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.ivSize, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new Key(KeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.ivSize, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
		} else {
			KeyPair keyPair = KeyGenerators.generateAsymmetricKey(this.keyAlgorithm);
			this.encryptionKey = new Key(KeyType.PRIVATE, this.keyAlgorithm, keyPair.getPrivate().getEncoded(), this.ivSize, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new Key(KeyType.PUBLIC, this.keyAlgorithm, keyPair.getPublic().getEncoded(), this.ivSize, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
		}
	}

	@Override
	public IKey getKeyForSigning() throws CryptoException {
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		return this.encryptionKey;
	}

	@Override
	public IKey getKeyForSignatureVerification() throws CryptoException {
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		return this.decryptionKey;
	}

	@Override
	public IKey getKeyForEncryption() throws CryptoException {
		return this.getKeyForSigning();
	}

	@Override
	public IKey getKeyForDecryption() throws CryptoException {
		return this.getKeyForSignatureVerification();
	}

	private void throwExceptionIfRevoked() throws CryptoException {
		if (this.revoked) {
			throw new CryptoException("The key for realm " + this.name + " is revoked");
		}
	}

	private void throwExceptionIfExpired() throws CryptoException {
		if (this.isExpired()) {
			throw new CryptoException("The key for realm " + this.name + " has expired");
		}
	}

	@Override
	public void revoke() {
		this.revoked = true;
		log.warn("Key realm {} has been revoked", this.name);
	}

	@Override
	public boolean isExpired() {
		return this.expiration != null && new Date().after(this.expiration);
	}

	@Override
	public IKeyRealm rotate() {
		var rotated = new KeyRealm(this.name, this.keyAlgorithm, this.expiration, this.ivSize,
				this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
		rotated.version = this.version + 1;
		log.debug("Key realm {} rotated to version {}", this.name, rotated.version);
		return rotated;
	}

}
