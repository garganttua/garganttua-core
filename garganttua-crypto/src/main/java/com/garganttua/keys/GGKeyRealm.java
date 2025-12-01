package com.garganttua.keys;

import java.security.KeyPair;
import java.util.Date;

import javax.crypto.SecretKey;

import lombok.Getter;

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
		this.name = keyRealmName;
		this.keyAlgorithm = keyAlgorithm;
		this.expiration = expiration;
		if( keyAlgorithm != null )
			this.type = keyAlgorithm.getType();
		if( initializationVectorSize > 0 ) {
			this.initializationVector = new byte[initializationVectorSize];
			GGKeyRandoms.secureRandom().nextBytes(this.initializationVector);
		}

		this.encryptionMode = encryptionMode;
		this.paddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		if( keyAlgorithm != null )
			this.createKeys();
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
		if( this.type == GGKeyRealmType.SYMETRIC) {
			SecretKey key = this.keyAlgorithm.generateSymetricKey();
			this.encryptionKey = new GGKey(GGKeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new GGKey(GGKeyType.SECRET, this.keyAlgorithm, key.getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
		} else {
			KeyPair keyPair = this.keyAlgorithm.generateAsymetricKey();
			this.encryptionKey = new GGKey(GGKeyType.PRIVATE, this.keyAlgorithm, keyPair.getPrivate().getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
			this.decryptionKey = new GGKey(GGKeyType.PUBLIC, this.keyAlgorithm, keyPair.getPublic().getEncoded(), this.initializationVector, this.encryptionMode, this.paddingMode, this.signatureAlgorithm);
		}
	}
	
	@Override
	public boolean equals(IGGKeyRealm object) {
		return false;
	}
	

	@Override
	public IGGKey getKeyForSigning() throws GGKeyException {
    	this.throwExceptionIfExpired();
    	this.throwExceptionIfRevoked();
		return this.encryptionKey;
	}

	@Override
	public IGGKey getKeyForSignatureVerification() throws GGKeyException {
    	this.throwExceptionIfExpired();
    	this.throwExceptionIfRevoked();
		return this.decryptionKey;
	}

    @Override
    public IGGKey getKeyForEncryption() throws GGKeyException {
    	this.throwExceptionIfExpired();
    	this.throwExceptionIfRevoked();
		return this.encryptionKey;
	}
    
    private void throwExceptionIfRevoked() throws GGKeyException {
    	if( this.revoked ) {
    		throw new GGKeyException("The key for realm "+this.name+" is revoked");
    	}
	}

	private void throwExceptionIfExpired() throws GGKeyException {
    	if( this.expiration != null && new Date().after(this.expiration) ) {
    		throw new GGKeyException("The key for realm "+this.name+" has expired");
    	}
	}

    @Override
	public IGGKey getKeyForDecryption() throws GGKeyException {
		this.throwExceptionIfExpired();
		this.throwExceptionIfRevoked();
		return this.decryptionKey;
    }

	@Override
	public void revoke() {
		this.revoked = true;
	}
}
