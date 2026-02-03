package com.garganttua.keys;

import java.security.KeyPair;
import java.util.Date;

import javax.crypto.SecretKey;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGKeyRealm implements IGGKeyRealm {
		
	//Ctr for encryption only

	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode) {
		this(keyRealmName, keyAlgorithm, null, -1, encryptionMode, paddingMode, null);
	}
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, Date expiration, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode) {
		this(keyRealmName, keyAlgorithm, expiration, -1, encryptionMode, paddingMode, null);
	}	
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm,  int initializationVectorSize, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode) {
		this(keyRealmName, keyAlgorithm, null, initializationVectorSize, encryptionMode, paddingMode, null);
	}
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, Date expiration, int initializationVectorSize, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode) {
		this(keyRealmName, keyAlgorithm, expiration, initializationVectorSize, encryptionMode, paddingMode, null);
	}
	
	//Ctr for signature only
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, GGSignatureAlgorithm signatureAlgorithm) {
		this(keyRealmName, keyAlgorithm, null, -1, null, null, signatureAlgorithm);
	}
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, Date expiration, GGSignatureAlgorithm signatureAlgorithm) {
		this(keyRealmName, keyAlgorithm, expiration, -1, null, null, signatureAlgorithm);
	}
	
	//Ctr for signature and encryption
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode, GGSignatureAlgorithm signatureAlgorithm) {
		this(keyRealmName, keyAlgorithm, null, -1, encryptionMode, paddingMode, signatureAlgorithm);
	}
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, int initializationVectorSize, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode, GGSignatureAlgorithm signatureAlgorithm) {
		this(keyRealmName, keyAlgorithm, null, initializationVectorSize, encryptionMode, paddingMode, signatureAlgorithm);
	}
	
	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, Date expiration, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode, GGSignatureAlgorithm signatureAlgorithm) {
		this(keyRealmName, keyAlgorithm, expiration, -1, encryptionMode, paddingMode, signatureAlgorithm);
	}
	
	//Complete CTR

	public GGKeyRealm(String keyRealmName, GGKeyAlgorithm keyAlgorithm, Date expiration, int initializationVectorSize, GGEncryptionMode encryptionMode,
			GGEncryptionPaddingMode paddingMode, GGSignatureAlgorithm signatureAlgorithm) {
		log.atTrace().log("Entering GGKeyRealm main constructor with keyRealmName={}, keyAlgorithm={}, expiration={}, initializationVectorSize={}, encryptionMode={}, paddingMode={}, signatureAlgorithm={}", keyRealmName, keyAlgorithm, expiration, initializationVectorSize, encryptionMode, paddingMode, signatureAlgorithm);
		this.name = keyRealmName;
		this.keyAlgorithm = keyAlgorithm;
		this.expiration = expiration;
		if( keyAlgorithm != null )
			this.type = keyAlgorithm.getType();
		if( initializationVectorSize > 0 ) {
			log.atDebug().log("Creating initialization vector of size {}", initializationVectorSize);
			this.initializationVector = new byte[initializationVectorSize];
			GGKeyRandoms.secureRandom().nextBytes(this.initializationVector);
		}

		this.encryptionMode = encryptionMode;
		this.paddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		if( keyAlgorithm != null )
			this.createKeys();
		log.atDebug().log("GGKeyRealm initialized with name={}, type={}", this.name, this.type);
		log.atTrace().log("Exiting GGKeyRealm main constructor");
	}
	@Getter
	protected String name;
	
	protected byte[] initializationVector;

	protected GGEncryptionMode encryptionMode;

	protected GGEncryptionPaddingMode paddingMode;

	protected GGSignatureAlgorithm signatureAlgorithm;

	@Getter
	protected GGKeyAlgorithm keyAlgorithm;
	
	@Getter
	protected GGKeyRealmType type;
	
	/**
	 * Actually, a private key, or a secret key
	 */
	protected GGKey encryptionKey;
	
	/**
	 * Actually, a public key, or a secret key
	 */
	protected GGKey decryptionKey;
	
	@Getter
	protected String ownerId;
	
	@Getter
	protected Date expiration;
	
	@Getter
	protected boolean revoked;
	
	private void createKeys() {
		log.atTrace().log("Entering createKeys with type={}", this.type);
		if( this.type == GGKeyRealmType.SYMETRIC) {
			log.atDebug().log("Creating symmetric keys for realm {}", this.name);
			SecretKey key = this.keyAlgorithm.generateSymetricKey();
			this.encryptionKey = new GGKey(GGKeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new GGKey(GGKeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			log.atDebug().log("Symmetric keys created for realm {}", this.name);
		} else {
			log.atDebug().log("Creating asymmetric key pair for realm {}", this.name);
			KeyPair keyPair = this.keyAlgorithm.generateAsymetricKey();
			this.encryptionKey = new GGKey(GGKeyType.PRIVATE, this.keyAlgorithm, keyPair.getPrivate().getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new GGKey(GGKeyType.PUBLIC, this.keyAlgorithm, keyPair.getPublic().getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			log.atDebug().log("Asymmetric key pair created for realm {}", this.name);
		}
		log.atTrace().log("Exiting createKeys");
	}
	
	@Override
	public boolean equals(IGGKeyRealm object) {
		return false;
	}
	

	@Override
	public IGGKey getKeyForSigning() throws GGKeyException {
		log.atTrace().log("Entering getKeyForSigning for realm {}", this.name);
    	this.throwExceptionIfExpired();
    	this.throwExceptionIfRevoked();
		log.atDebug().log("Returning signing key for realm {}", this.name);
		log.atTrace().log("Exiting getKeyForSigning");
		return this.encryptionKey;
	}

	@Override
	public IGGKey getKeyForSignatureVerification() throws GGKeyException {
		log.atTrace().log("Entering getKeyForSignatureVerification for realm {}", this.name);
    	this.throwExceptionIfExpired();
    	this.throwExceptionIfRevoked();
		log.atDebug().log("Returning signature verification key for realm {}", this.name);
		log.atTrace().log("Exiting getKeyForSignatureVerification");
		return this.decryptionKey;
	}

    @Override
    public IGGKey getKeyForEncryption() throws GGKeyException {
		log.atTrace().log("Entering getKeyForEncryption for realm {}", this.name);
    	this.throwExceptionIfExpired();
    	this.throwExceptionIfRevoked();
		log.atDebug().log("Returning encryption key for realm {}", this.name);
		log.atTrace().log("Exiting getKeyForEncryption");
		return this.encryptionKey;
	}
    
    private void throwExceptionIfRevoked() throws GGKeyException {
		log.atTrace().log("Checking if key realm {} is revoked", this.name);
    	if( this.revoked ) {
			log.atError().log("Key realm {} is revoked", this.name);
    		throw new GGKeyException("The key for realm "+this.name+" is revoked");
    	}
	}

	private void throwExceptionIfExpired() throws GGKeyException {
		log.atTrace().log("Checking if key realm {} is expired", this.name);
    	if( this.expiration != null && new Date().after(this.expiration) ) {
			log.atError().log("Key realm {} has expired at {}", this.name, this.expiration);
    		throw new GGKeyException("The key for realm "+this.name+" has expired");
    	}
	}

    @Override
	public IGGKey getKeyForDecryption() throws GGKeyException {
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
