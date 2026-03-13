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

	protected byte[] initializationVector;

	protected EncryptionMode encryptionMode;

	protected EncryptionPaddingMode paddingMode;

	protected SignatureAlgorithm signatureAlgorithm;

	@Getter
	protected KeyAlgorithm keyAlgorithm;

	@Getter
	protected KeyAlgorithmType type;

	protected Key encryptionKey;

	protected Key decryptionKey;

	@Getter
	protected String ownerId;

	@Getter
	protected Date expiration;

	@Getter
	protected boolean revoked;

	KeyRealm(String name, KeyAlgorithm keyAlgorithm, Date expiration, int initializationVectorSize,
			EncryptionMode encryptionMode, EncryptionPaddingMode paddingMode, SignatureAlgorithm signatureAlgorithm) {
		log.atTrace().log("Entering KeyRealm constructor with name={}, keyAlgorithm={}, expiration={}, initializationVectorSize={}, encryptionMode={}, paddingMode={}, signatureAlgorithm={}", name, keyAlgorithm, expiration, initializationVectorSize, encryptionMode, paddingMode, signatureAlgorithm);
		this.name = name;
		this.keyAlgorithm = keyAlgorithm;
		this.expiration = expiration;
		if (keyAlgorithm != null)
			this.type = keyAlgorithm.getType();
		if (initializationVectorSize > 0) {
			log.atDebug().log("Creating initialization vector of size {}", initializationVectorSize);
			this.initializationVector = new byte[initializationVectorSize];
			KeyRandoms.secureRandom().nextBytes(this.initializationVector);
		}

		this.encryptionMode = encryptionMode;
		this.paddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		if (keyAlgorithm != null)
			this.createKeys();
		log.atDebug().log("KeyRealm initialized with name={}, type={}", this.name, this.type);
		log.atTrace().log("Exiting KeyRealm constructor");
	}

	private void createKeys() {
		log.atTrace().log("Entering createKeys with type={}", this.type);
		if (this.type == KeyAlgorithmType.SYMMETRIC) {
			log.atDebug().log("Creating symmetric keys for realm {}", this.name);
			SecretKey key = this.keyAlgorithm.generateSymmetricKey();
			this.encryptionKey = new Key(KeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new Key(KeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			log.atDebug().log("Symmetric keys created for realm {}", this.name);
		} else {
			log.atDebug().log("Creating asymmetric key pair for realm {}", this.name);
			KeyPair keyPair = this.keyAlgorithm.generateAsymmetricKey();
			this.encryptionKey = new Key(KeyType.PRIVATE, this.keyAlgorithm, keyPair.getPrivate().getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new Key(KeyType.PUBLIC, this.keyAlgorithm, keyPair.getPublic().getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			log.atDebug().log("Asymmetric key pair created for realm {}", this.name);
		}
		log.atTrace().log("Exiting createKeys");
	}

	@Override
	public IKey getKeyForSigning() throws CryptoException {
		log.atTrace().log("Entering getKeyForSigning for realm {}", this.name);
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		log.atDebug().log("Returning signing key for realm {}", this.name);
		log.atTrace().log("Exiting getKeyForSigning");
		return this.encryptionKey;
	}

	@Override
	public IKey getKeyForSignatureVerification() throws CryptoException {
		log.atTrace().log("Entering getKeyForSignatureVerification for realm {}", this.name);
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		log.atDebug().log("Returning signature verification key for realm {}", this.name);
		log.atTrace().log("Exiting getKeyForSignatureVerification");
		return this.decryptionKey;
	}

	@Override
	public IKey getKeyForEncryption() throws CryptoException {
		log.atTrace().log("Entering getKeyForEncryption for realm {}", this.name);
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		log.atDebug().log("Returning encryption key for realm {}", this.name);
		log.atTrace().log("Exiting getKeyForEncryption");
		return this.encryptionKey;
	}

	private void throwExceptionIfRevoked() throws CryptoException {
		log.atTrace().log("Checking if key realm {} is revoked", this.name);
		if (this.revoked) {
			log.atError().log("Key realm {} is revoked", this.name);
			throw new CryptoException("The key for realm " + this.name + " is revoked");
		}
	}

	private void throwExceptionIfExpired() throws CryptoException {
		log.atTrace().log("Checking if key realm {} is expired", this.name);
		if (this.expiration != null && new Date().after(this.expiration)) {
			log.atError().log("Key realm {} has expired at {}", this.name, this.expiration);
			throw new CryptoException("The key for realm " + this.name + " has expired");
		}
	}

	@Override
	public IKey getKeyForDecryption() throws CryptoException {
		log.atTrace().log("Entering getKeyForDecryption for realm {}", this.name);
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		log.atDebug().log("Returning decryption key for realm {}", this.name);
		log.atTrace().log("Exiting getKeyForDecryption");
		return this.decryptionKey;
	}

	@Override
	public void revoke() {
		log.atTrace().log("Entering revoke for realm {}", this.name);
		this.revoked = true;
		log.atWarn().log("Key realm {} has been revoked", this.name);
		log.atTrace().log("Exiting revoke");
	}
}
