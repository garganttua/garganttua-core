package com.garganttua.core.crypto;

import java.util.Base64;

public class KeySerializer {

	public static String exportRawKey(IKey key) {
		return new String(key.getRawKey());
	}

	public static IKey importRawKey(String base64RawKey, KeyType type, IKeyAlgorithm algorithm,
			int ivSize, EncryptionMode encryptionMode, EncryptionPaddingMode paddingMode,
			SignatureAlgorithm signatureAlgorithm) {
		byte[] decoded = Base64.getDecoder().decode(base64RawKey);
		return new Key(type, algorithm, decoded, ivSize, encryptionMode, paddingMode, signatureAlgorithm);
	}

	private KeySerializer() {
	}

}
