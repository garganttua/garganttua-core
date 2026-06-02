package com.garganttua.core.crypto;

import com.garganttua.core.observability.Logger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.spec.SecretKeySpec;

/**
 * Immutable {@link IKey} implementation that stores its raw material Base64-encoded
 * and lazily reconstructs the corresponding JDK {@link java.security.Key} on first
 * use (cached via double-checked locking). Delegates encryption/decryption to
 * {@link Encryptor} and signing/verification to {@link Signer}.
 *
 * <p>Equality is defined solely on the stored raw key bytes.
 */
public class Key implements IKey {
    private static final Logger log = Logger.getLogger(Key.class);

	private final KeyType type;

	private final IKeyAlgorithm algorithm;

	private final byte[] rawKey;

	private final int ivSize;

	private final EncryptionMode encryptionMode;

	private final EncryptionPaddingMode encryptionPaddingMode;

	private final SignatureAlgorithm signatureAlgorithm;

	/**
	 * Lazily-cached JDK {@code java.security.Key} reconstructed from {@link #rawKey}.
	 * Volatile so the double-checked-locking pattern in {@link #getKey()} is safe
	 * under Java 5+ memory model. Reset to {@code null} only on initial construction
	 * (Key is otherwise immutable).
	 */
	private volatile java.security.Key cachedJdkKey;

	Key(KeyType type, IKeyAlgorithm algorithm, byte[] rawKey, int ivSize,
			EncryptionMode encryptionMode, EncryptionPaddingMode paddingMode,
			SignatureAlgorithm signatureAlgorithm) {
		this.type = type;
		this.algorithm = algorithm;
		this.encryptionMode = encryptionMode;
		this.encryptionPaddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		this.rawKey = Base64.getEncoder().encode(rawKey);
		this.ivSize = ivSize;
		log.debug("Key created with type={}, algorithm={}", this.type, this.algorithm);
	}

	/**
	 * Reconstruct a signing/verifying {@link Key} from persisted JDK-encoded material.
	 * Inverse path of the in-memory ctor used by {@code KeyRealmBuilder.build()}.
	 *
	 * <p>Input bytes must be in the standard JDK encoding for the key type:
	 * <ul>
	 *   <li>{@link KeyType#PRIVATE} → PKCS#8 ({@code privateKey.getEncoded()})</li>
	 *   <li>{@link KeyType#PUBLIC} → X.509 ({@code publicKey.getEncoded()})</li>
	 * </ul>
	 *
	 * <p><b>Round-trip note:</b> {@link #getRawKey()} returns Base64-encoded bytes
	 * (legacy contract). If you persisted {@code getRawKey()} output, Base64-decode
	 * before calling this method.
	 *
	 * @param type                {@link KeyType#PRIVATE} or {@link KeyType#PUBLIC}
	 *                            ({@link KeyType#SECRET} is rejected — use
	 *                            {@link #fromEncryptionMaterial})
	 * @param algorithm           algorithm the bytes were generated for
	 * @param signatureAlgorithm  signature algorithm for sign/verify
	 * @param material            JDK-encoded bytes
	 * @return a {@link Key} ready for {@code sign} / {@code verifySignature}
	 * @since 2.0.0-ALPHA02
	 */
	public static Key fromSigningMaterial(KeyType type, IKeyAlgorithm algorithm,
			SignatureAlgorithm signatureAlgorithm, byte[] material) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(algorithm, "algorithm");
		Objects.requireNonNull(signatureAlgorithm, "signatureAlgorithm");
		Objects.requireNonNull(material, "material");
		if (type == KeyType.SECRET) {
			throw new IllegalArgumentException(
					"fromSigningMaterial does not support SECRET keys — use fromEncryptionMaterial");
		}
		return new Key(type, algorithm, material, 0, null, null, signatureAlgorithm);
	}

	/**
	 * Reconstruct an encrypting/decrypting {@link Key} from persisted JDK-encoded
	 * material. Symmetric algorithms produce a {@link KeyType#SECRET} key; asymmetric
	 * algorithms produce {@link KeyType#PRIVATE} or {@link KeyType#PUBLIC} per the
	 * {@code type} argument.
	 *
	 * <p>See {@link #fromSigningMaterial} for the Base64 round-trip caveat.
	 *
	 * @param type      key type matching the algorithm family
	 * @param algorithm algorithm the bytes were generated for
	 * @param mode      encryption mode (block-cipher mode of operation)
	 * @param padding   padding scheme
	 * @param ivSize    IV size in bytes (0 if not applicable)
	 * @param material  JDK-encoded bytes (PKCS#8 / X.509 / raw secret)
	 * @return a {@link Key} ready for {@code encrypt} / {@code decrypt}
	 * @since 2.0.0-ALPHA02
	 */
	public static Key fromEncryptionMaterial(KeyType type, IKeyAlgorithm algorithm,
			EncryptionMode mode, EncryptionPaddingMode padding, int ivSize, byte[] material) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(algorithm, "algorithm");
		Objects.requireNonNull(mode, "mode");
		Objects.requireNonNull(padding, "padding");
		Objects.requireNonNull(material, "material");
		return new Key(type, algorithm, material, ivSize, mode, padding, null);
	}

	/**
	 * Reconstructs (or returns the cached) JDK {@link java.security.Key} from the
	 * stored raw material. Thread-safe and computed at most once.
	 *
	 * @return the reconstructed JDK key
	 * @throws CryptoException if the stored material cannot be decoded into a key
	 */
	@Override
	public java.security.Key getKey() throws CryptoException {
		// Fast path: cache hit (no synchronization required, volatile read suffices).
		java.security.Key cached = this.cachedJdkKey;
		if (cached != null) {
			return cached;
		}
		// Slow path: reconstruct once under the monitor. Re-check inside the lock
		// so a concurrent first caller doesn't reconstruct twice.
		synchronized (this) {
			if (this.cachedJdkKey != null) {
				return this.cachedJdkKey;
			}
			byte[] decodedRawKey = Base64.getDecoder().decode(this.rawKey);
			try {
				java.security.Key built = switch (this.type) {
					case SECRET -> new SecretKeySpec(decodedRawKey, 0, decodedRawKey.length, this.algorithm.getName());
					case PRIVATE -> KeyFactory.getInstance(this.algorithm.getName())
							.generatePrivate(new PKCS8EncodedKeySpec(decodedRawKey));
					case PUBLIC -> KeyFactory.getInstance(this.algorithm.getName())
							.generatePublic(new X509EncodedKeySpec(decodedRawKey));
				};
				this.cachedJdkKey = built;
				return built;
			} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
				throw new CryptoException("Failed to reconstruct key", e);
			}
		}
	}

	/**
	 * Encrypts {@code clear} using this key's algorithm, mode and padding. When an
	 * IV size is configured, a fresh IV is generated and prepended to the output.
	 *
	 * @param clear the plaintext bytes
	 * @return the ciphertext (IV prefixed when applicable)
	 * @throws CryptoException if encryption fails
	 */
	@Override
	public byte[] encrypt(byte[] clear) throws CryptoException {
		String cipherName = this.algorithm.getCipherName(this.encryptionMode, this.encryptionPaddingMode);
		return Encryptor.encrypt(this.getKey(), cipherName, this.encryptionMode, this.ivSize, clear);
	}

	/**
	 * Decrypts {@code encoded}, consuming the prepended IV when an IV size is
	 * configured.
	 *
	 * @param encoded the ciphertext (IV prefixed when applicable)
	 * @return the recovered plaintext bytes
	 * @throws CryptoException if decryption fails
	 */
	@Override
	public byte[] decrypt(byte[] encoded) throws CryptoException {
		String cipherName = this.algorithm.getCipherName(this.encryptionMode, this.encryptionPaddingMode);
		return Encryptor.decrypt(this.getKey(), cipherName, this.encryptionMode, this.ivSize, encoded);
	}

	/**
	 * Signs {@code data} with this private key.
	 *
	 * @param data the bytes to sign
	 * @return the signature bytes
	 * @throws CryptoException if this is not a {@link KeyType#PRIVATE} key or signing fails
	 */
	@Override
	public byte[] sign(byte[] data) throws CryptoException {
		if (this.type != KeyType.PRIVATE) {
			throw new CryptoException("Cannot sign with other than Private key");
		}
		String sigName = this.algorithm.getSignatureName(this.signatureAlgorithm);
		return Signer.sign((PrivateKey) this.getKey(), sigName, data);
	}

	/**
	 * Verifies {@code signature} against {@code originalData} with this public key.
	 *
	 * @param signature    the signature to check
	 * @param originalData the data that was supposedly signed
	 * @return {@code true} if the signature is valid
	 * @throws CryptoException if this is not a {@link KeyType#PUBLIC} key or verification fails
	 */
	@Override
	public boolean verifySignature(byte[] signature, byte[] originalData) throws CryptoException {
		if (this.type != KeyType.PUBLIC) {
			throw new CryptoException("Cannot verify signature with other than Public key");
		}
		String sigName = this.algorithm.getSignatureName(this.signatureAlgorithm);
		return Signer.verify((PublicKey) this.getKey(), sigName, signature, originalData);
	}

	/** {@return the Base64-encoded raw key bytes (legacy contract)} */
	@Override
	public byte[] getRawKey() {
		return this.rawKey;
	}

	/** {@return the algorithm this key was generated for} */
	@Override
	public IKeyAlgorithm getAlgorithm() {
		return this.algorithm;
	}

	/** {@return the key type (secret, private or public)} */
	@Override
	public KeyType getType() {
		return this.type;
	}

	/** {@return the configured encryption mode, or {@code null} for signing keys} */
	@Override
	public EncryptionMode getEncryptionMode() {
		return this.encryptionMode;
	}

	/** {@return the configured padding scheme, or {@code null} for signing keys} */
	@Override
	public EncryptionPaddingMode getEncryptionPaddingMode() {
		return this.encryptionPaddingMode;
	}

	/** {@return the configured signature algorithm, or {@code null} for encryption keys} */
	@Override
	public SignatureAlgorithm getSignatureAlgorithm() {
		return this.signatureAlgorithm;
	}

	/**
	 * Compares two keys by their raw key bytes.
	 *
	 * @param obj the object to compare with
	 * @return {@code true} if {@code obj} is a {@link Key} with identical raw bytes
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Key other = (Key) obj;
		return Arrays.equals(rawKey, other.rawKey);
	}

	/** {@return a hash code derived from the raw key bytes} */
	@Override
	public int hashCode() {
		return Arrays.hashCode(rawKey);
	}

}
