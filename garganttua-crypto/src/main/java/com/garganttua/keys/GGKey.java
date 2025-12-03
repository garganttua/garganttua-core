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
		log.atTrace().log("Entering GGKey constructor with type={}, algorithm={}, encryptionMode={}, paddingMode={}, signatureAlgorithm={}", type, algorithm, encryptionMode, paddingMode, signatureAlgorithm);
		this.type = type;
		this.algorithm = algorithm;
		this.encryptionMode = encryptionMode;
		this.encryptionPaddingMode = paddingMode;
		this.signatureAlgorithm = signatureAlgorithm;
		Encoder b64Encoder = Base64.getEncoder();
		this.rawKey = b64Encoder.encode(rawKey);
		this.initializationVector = initializationVector;
		log.atDebug().log("GGKey created with type={}, algorithm={}", this.type, this.algorithm);
		log.atTrace().log("Exiting GGKey constructor");
	}

	@Override
	public boolean equals(Object obj) {
		return Arrays.equals(rawKey, ((GGKey) obj).rawKey);
	}

	@Override
	public Key getKey() throws GGKeyException {
		log.atTrace().log("Entering getKey with type={}", this.type);
		Key key_ = null;
		Decoder b64Decoder = Base64.getDecoder();
		byte[] decodedRawKey = b64Decoder.decode(this.rawKey);
		try {
			if (this.type == GGKeyType.SECRET) {
				log.atDebug().log("Creating SecretKey for algorithm {}", this.algorithm);
				key_ = new SecretKeySpec(decodedRawKey, 0, decodedRawKey.length, this.algorithm.getAlgorithm());
			}
			if (this.type == GGKeyType.PRIVATE) {
				log.atDebug().log("Creating PrivateKey for algorithm {}", this.algorithm);
				key_ = KeyFactory.getInstance(this.algorithm.getAlgorithm())
						.generatePrivate(new PKCS8EncodedKeySpec(decodedRawKey));
			}
			if (this.type == GGKeyType.PUBLIC) {
				log.atDebug().log("Creating PublicKey for algorithm {}", this.algorithm);
				key_ = KeyFactory.getInstance(this.algorithm.getAlgorithm())
						.generatePublic(new X509EncodedKeySpec(decodedRawKey));
			}
			log.atInfo().log("Key retrieved successfully for type={}, algorithm={}", this.type, this.algorithm);
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			log.atWarn().log("Error in getting keys from bytes", e);
			throw new GGKeyException(e);
		}
		log.atTrace().log("Exiting getKey");
		return key_;
	}

	@Override
	public byte[] encrypt(byte[] clear)
			throws GGKeyException {
		log.atTrace().log("Entering encrypt with data length={}", clear != null ? clear.length : 0);
		Cipher cipher;
		try {
			String cipherName = this.algorithm.geCipherName(this.encryptionMode, this.encryptionPaddingMode);
			log.atDebug().log("Initializing cipher {} for encryption", cipherName);
			cipher = Cipher.getInstance(cipherName);
			if( this.initializationVector != null )
				if( this.encryptionMode == GGEncryptionMode.GCM )
					cipher.init(Cipher.ENCRYPT_MODE, this.getKey(), new GCMParameterSpec(128, this.initializationVector));
				else
					cipher.init(Cipher.ENCRYPT_MODE, this.getKey(), new IvParameterSpec(this.initializationVector));
			else
				cipher.init(Cipher.ENCRYPT_MODE, this.getKey());
			byte[] encrypted = cipher.doFinal(clear);
			log.atInfo().log("Data encrypted successfully, output length={}", encrypted.length);
			log.atTrace().log("Exiting encrypt");
			return encrypted;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
				| InvalidKeyException | InvalidAlgorithmParameterException e) {
			log.atWarn().log("Encryption error", e);
			throw new GGKeyException(e);
		}
	}

	@Override
	public byte[] decrypt(byte[] encoded)
			throws GGKeyException {
		log.atTrace().log("Entering decrypt with data length={}", encoded != null ? encoded.length : 0);
		Cipher cipher;
		try {
			String cipherName = this.algorithm.geCipherName(this.encryptionMode, this.encryptionPaddingMode);
			log.atDebug().log("Initializing cipher {} for decryption", cipherName);
			cipher = Cipher.getInstance(cipherName);
			if( this.initializationVector != null )
				if( this.encryptionMode == GGEncryptionMode.GCM )
					cipher.init(Cipher.DECRYPT_MODE, this.getKey(), new GCMParameterSpec(128, this.initializationVector));
				else
					cipher.init(Cipher.DECRYPT_MODE, this.getKey(), new IvParameterSpec(this.initializationVector));
			else
				cipher.init(Cipher.DECRYPT_MODE, this.getKey());
			byte[] decrypted = cipher.doFinal(encoded);
			log.atInfo().log("Data decrypted successfully, output length={}", decrypted.length);
			log.atTrace().log("Exiting decrypt");
			return decrypted;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
				| InvalidKeyException | InvalidAlgorithmParameterException e) {
			log.atWarn().log("Decryption error", e);
			throw new GGKeyException(e);
		}
	}

	@Override
	public byte[] sign(byte[] data) throws GGKeyException {
		log.atTrace().log("Entering sign with data length={}", data != null ? data.length : 0);
		if (this.type != GGKeyType.PRIVATE) {
			log.atError().log("Attempt to sign with non-private key: type={}", this.type);
			throw new GGKeyException(
					"Cannot sign with other than Private key");
		}
		try {
			String geSignatureName = this.algorithm.geSignatureName(this.signatureAlgorithm);
			log.atDebug().log("Creating signature with algorithm {}", geSignatureName);
			Signature signature = Signature.getInstance(geSignatureName);
			signature.initSign((PrivateKey) this.getKey());
			signature.update(data);
			byte[] signatureBytes = signature.sign();
			log.atInfo().log("Data signed successfully, signature length={}", signatureBytes.length);
			log.atTrace().log("Exiting sign");
			return signatureBytes;
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			log.atWarn().log("Signature error", e);
			throw new GGKeyException("Signature error", e);
		}
	}

	@Override
	public boolean verifySignature(byte[] signature, byte[] originalData)
			throws GGKeyException {
		log.atTrace().log("Entering verifySignature with signature length={}, data length={}", signature != null ? signature.length : 0, originalData != null ? originalData.length : 0);
		if (this.type != GGKeyType.PUBLIC) {
			log.atError().log("Attempt to verify signature with non-public key: type={}", this.type);
			throw new GGKeyException(
					"Cannot verify signature with other than Public key");
		}
		try {
			String signatureName = this.algorithm.geSignatureName(this.signatureAlgorithm);
			log.atDebug().log("Verifying signature with algorithm {}", signatureName);
			Signature signatureVerify = Signature.getInstance(signatureName);
			signatureVerify.initVerify((PublicKey) this.getKey());
			signatureVerify.update(originalData);
			boolean isValid = signatureVerify.verify(signature);
			log.atInfo().log("Signature verification result: {}", isValid);
			log.atTrace().log("Exiting verifySignature");
			return isValid;
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			log.atWarn().log("Signature verification error", e);
			throw new GGKeyException("Signature verification error",
					e);
		}
	}

}
