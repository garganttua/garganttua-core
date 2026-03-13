package com.garganttua.core.crypto;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Key implements IKey {

	@Getter
	private final KeyType type;

	private final IKeyAlgorithm algorithm;

	private final byte[] rawKey;

	private final int ivSize;

	@Getter
	private final EncryptionMode encryptionMode;

	@Getter
	private final EncryptionPaddingMode encryptionPaddingMode;

	@Getter
	private final SignatureAlgorithm signatureAlgorithm;

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
		log.atDebug().log("Key created with type={}, algorithm={}", this.type, this.algorithm);
	}

	@Override
	public java.security.Key getKey() throws CryptoException {
		byte[] decodedRawKey = Base64.getDecoder().decode(this.rawKey);
		try {
			return switch (this.type) {
				case SECRET -> new SecretKeySpec(decodedRawKey, 0, decodedRawKey.length, this.algorithm.getName());
				case PRIVATE -> KeyFactory.getInstance(this.algorithm.getName())
						.generatePrivate(new PKCS8EncodedKeySpec(decodedRawKey));
				case PUBLIC -> KeyFactory.getInstance(this.algorithm.getName())
						.generatePublic(new X509EncodedKeySpec(decodedRawKey));
			};
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new CryptoException("Failed to reconstruct key", e);
		}
	}

	@Override
	public byte[] encrypt(byte[] clear) throws CryptoException {
		String cipherName = this.algorithm.getCipherName(this.encryptionMode, this.encryptionPaddingMode);
		return Encryptor.encrypt(this.getKey(), cipherName, this.encryptionMode, this.ivSize, clear);
	}

	@Override
	public byte[] decrypt(byte[] encoded) throws CryptoException {
		String cipherName = this.algorithm.getCipherName(this.encryptionMode, this.encryptionPaddingMode);
		return Encryptor.decrypt(this.getKey(), cipherName, this.encryptionMode, this.ivSize, encoded);
	}

	@Override
	public byte[] sign(byte[] data) throws CryptoException {
		if (this.type != KeyType.PRIVATE) {
			throw new CryptoException("Cannot sign with other than Private key");
		}
		String sigName = this.algorithm.getSignatureName(this.signatureAlgorithm);
		return Signer.sign((PrivateKey) this.getKey(), sigName, data);
	}

	@Override
	public boolean verifySignature(byte[] signature, byte[] originalData) throws CryptoException {
		if (this.type != KeyType.PUBLIC) {
			throw new CryptoException("Cannot verify signature with other than Public key");
		}
		String sigName = this.algorithm.getSignatureName(this.signatureAlgorithm);
		return Signer.verify((PublicKey) this.getKey(), sigName, signature, originalData);
	}

	@Override
	public byte[] getRawKey() {
		return this.rawKey;
	}

	@Override
	public IKeyAlgorithm getAlgorithm() {
		return this.algorithm;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Key other = (Key) obj;
		return Arrays.equals(rawKey, other.rawKey);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(rawKey);
	}

}
