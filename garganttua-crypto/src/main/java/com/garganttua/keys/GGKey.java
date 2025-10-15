package com.garganttua.keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@NoArgsConstructor
@Slf4j
public class GGKey implements IGGKey {

	private GGKeyType type;

	private GGKeyAlgorithm algorithm;

	/**
	 * Base64 Encoded
	 */
	private byte[] rawKey;

	private byte[] initializationVector;

	private GGEncryptionMode encryptionMode;

	private GGEncryptionPaddingMode encryptionPaddingMode;

	private GGSignatureAlgorithm signatureAlgorithm;
	
	public GGKey(GGKeyType type, GGKeyAlgorithm algorithm, byte[] rawKey, byte[] initializationVector, GGEncryptionMode encryptionMode, GGEncryptionPaddingMode paddingMode, GGSignatureAlgorithm signatureAlgorithm) {
		super();
		this.type = type;
		this.algorithm = algorithm;
		this.encryptionMode = encryptionMode;
		this.encryptionPaddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		Encoder b64Encoder = Base64.getEncoder();
		this.rawKey = b64Encoder.encode(rawKey);
		this.initializationVector = initializationVector;
	}

	@Override
	public boolean equals(Object obj) {
		return Arrays.equals(rawKey, ((GGKey) obj).rawKey);
	}

	@Override
	public Key getKey() throws GGKeyException {
		Key key_ = null;
		Decoder b64Decoder = Base64.getDecoder();
		byte[] decodedRawKey = b64Decoder.decode(this.rawKey);
		try {
			if (this.type == GGKeyType.SECRET) {
				key_ = new SecretKeySpec(decodedRawKey, 0, decodedRawKey.length, this.algorithm.getAlgorithm());
			}
			if (this.type == GGKeyType.PRIVATE) {
				key_ = KeyFactory.getInstance(this.algorithm.getAlgorithm())
						.generatePrivate(new PKCS8EncodedKeySpec(decodedRawKey));
			}
			if (this.type == GGKeyType.PUBLIC) {
				key_ = KeyFactory.getInstance(this.algorithm.getAlgorithm())
						.generatePublic(new X509EncodedKeySpec(decodedRawKey));
			}
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			log.atWarn().log("Error in getting keys from bytes", e);
			throw new GGKeyException(e);
		}
		return key_;
	}

	@Override
	public byte[] encrypt(byte[] clear)
			throws GGKeyException {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(this.algorithm.geCipherName(this.encryptionMode, this.encryptionPaddingMode));
			if( this.initializationVector != null )
				if( this.encryptionMode == GGEncryptionMode.GCM )
					cipher.init(Cipher.ENCRYPT_MODE, this.getKey(), new GCMParameterSpec(128, this.initializationVector));
				else 	
					cipher.init(Cipher.ENCRYPT_MODE, this.getKey(), new IvParameterSpec(this.initializationVector));
			else 
				cipher.init(Cipher.ENCRYPT_MODE, this.getKey());
			return cipher.doFinal(clear);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
				| InvalidKeyException | InvalidAlgorithmParameterException e) {
			log.atWarn().log("Encryption error", e);
			throw new GGKeyException(e);
		}
	}

	@Override
	public byte[] decrypt(byte[] encoded)
			throws GGKeyException {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(this.algorithm.geCipherName(this.encryptionMode, this.encryptionPaddingMode));
			if( this.initializationVector != null )
				if( this.encryptionMode == GGEncryptionMode.GCM )
					cipher.init(Cipher.DECRYPT_MODE, this.getKey(), new GCMParameterSpec(128, this.initializationVector));
				else 	
					cipher.init(Cipher.DECRYPT_MODE, this.getKey(), new IvParameterSpec(this.initializationVector));
			else 
				cipher.init(Cipher.DECRYPT_MODE, this.getKey());
			return cipher.doFinal(encoded);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
				| InvalidKeyException | InvalidAlgorithmParameterException e) {
			log.atWarn().log("Decryption error", e);
			throw new GGKeyException(e);
		}
	}

	@Override
	public byte[] sign(byte[] data) throws GGKeyException {
		if (this.type != GGKeyType.PRIVATE) {
			throw new GGKeyException(
					"Cannot sign with other than Private key");
		}
		try {
			String geSignatureName = this.algorithm.geSignatureName(this.signatureAlgorithm);
			Signature signature = Signature.getInstance(geSignatureName);
			signature.initSign((PrivateKey) this.getKey());
			signature.update(data);
			return signature.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			log.atWarn().log("Signature error", e);
			throw new GGKeyException("Signature error", e);
		}
	}

	@Override
	public boolean verifySignature(byte[] signature, byte[] originalData)
			throws GGKeyException {
		if (this.type != GGKeyType.PUBLIC) {
			throw new GGKeyException(
					"Cannot verify signature with other than Public key");
		}
		try {
			Signature signatureVerify = Signature.getInstance(this.algorithm.geSignatureName(this.signatureAlgorithm));
			signatureVerify.initVerify((PublicKey) this.getKey());
			signatureVerify.update(originalData);
			return signatureVerify.verify(signature);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			log.atWarn().log("Signature verification error", e);
			throw new GGKeyException("Signature verification error",
					e);
		}
	}

}
