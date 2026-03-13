package com.garganttua.core.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class Encryptor {

	private static final int GCM_TAG_LENGTH_BITS = 128;

	static byte[] encrypt(java.security.Key key, String cipherName, EncryptionMode mode, int ivSize, byte[] data) throws CryptoException {
		log.atDebug().log("Encrypting with cipher={}, mode={}, ivSize={}", cipherName, mode, ivSize);
		try {
			Cipher cipher = Cipher.getInstance(cipherName);
			if (ivSize > 0) {
				byte[] iv = new byte[ivSize];
				KeyRandoms.secureRandom().nextBytes(iv);
				cipher.init(Cipher.ENCRYPT_MODE, key, createParameterSpec(mode, iv));
				byte[] ciphertext = cipher.doFinal(data);
				byte[] result = new byte[iv.length + ciphertext.length];
				System.arraycopy(iv, 0, result, 0, iv.length);
				System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
				return result;
			} else {
				cipher.init(Cipher.ENCRYPT_MODE, key);
				return cipher.doFinal(data);
			}
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new CryptoException("Encryption error", e);
		}
	}

	static byte[] decrypt(java.security.Key key, String cipherName, EncryptionMode mode, int ivSize, byte[] data) throws CryptoException {
		log.atDebug().log("Decrypting with cipher={}, mode={}, ivSize={}", cipherName, mode, ivSize);
		try {
			Cipher cipher = Cipher.getInstance(cipherName);
			if (ivSize > 0) {
				byte[] iv = new byte[ivSize];
				System.arraycopy(data, 0, iv, 0, ivSize);
				byte[] ciphertext = new byte[data.length - ivSize];
				System.arraycopy(data, ivSize, ciphertext, 0, ciphertext.length);
				cipher.init(Cipher.DECRYPT_MODE, key, createParameterSpec(mode, iv));
				return cipher.doFinal(ciphertext);
			} else {
				cipher.init(Cipher.DECRYPT_MODE, key);
				return cipher.doFinal(data);
			}
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new CryptoException("Decryption error", e);
		}
	}

	private static java.security.spec.AlgorithmParameterSpec createParameterSpec(EncryptionMode mode, byte[] iv) {
		if (mode == EncryptionMode.GCM) {
			return new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
		}
		return new IvParameterSpec(iv);
	}

}
