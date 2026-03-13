package com.garganttua.core.crypto;

import java.security.KeyPair;
import java.util.Date;

import javax.crypto.SecretKey;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeyRealm implements IKeyRealm {

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
		log.atDebug().log("Creating KeyRealm name={}, algorithm={}, ivSize={}, mode={}", name, keyAlgorithm, initializationVectorSize, encryptionMode);
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
		log.atDebug().log("KeyRealm initialized name={}, type={}", this.name, this.type);
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
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		return this.encryptionKey;
	}

	@Override
	public IKey getKeyForDecryption() throws CryptoException {
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		return this.decryptionKey;
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
		log.atWarn().log("Key realm {} has been revoked", this.name);
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
		log.atDebug().log("Key realm {} rotated to version {}", this.name, rotated.version);
		return rotated;
	}

}
