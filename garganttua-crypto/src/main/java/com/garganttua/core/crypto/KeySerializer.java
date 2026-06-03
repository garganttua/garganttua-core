package com.garganttua.core.crypto;

import java.util.Base64;

/**
 * Converts {@link IKey} instances to and from their Base64 raw-key string form,
 * for persistence or transport.
 */
public class KeySerializer {

	/**
	 * Exports a key's raw material as a Base64 string.
	 *
	 * @param key the key to export
	 * @return the Base64-encoded raw key
	 */
	public static String exportRawKey(IKey key) {
		// getRawKey() already returns Base64-encoded (ASCII) bytes (legacy contract),
		// so decode them to the String form with an explicit lossless charset.
		return new String(key.getRawKey(), java.nio.charset.StandardCharsets.US_ASCII);
	}

	/**
	 * Reconstructs a {@link Key} from a Base64-encoded raw key string and the
	 * cipher/signature parameters it was created with.
	 *
	 * @param base64RawKey       the Base64-encoded raw key material
	 * @param type               the key type (secret, private or public)
	 * @param algorithm          the key algorithm
	 * @param ivSize             IV size in bytes (0 if not applicable)
	 * @param encryptionMode     the encryption mode, or {@code null} for signing keys
	 * @param paddingMode        the padding scheme, or {@code null} for signing keys
	 * @param signatureAlgorithm the signature algorithm, or {@code null} for encryption keys
	 * @return the reconstructed key
	 */
	public static IKey importRawKey(String base64RawKey, KeyType type, IKeyAlgorithm algorithm,
			int ivSize, EncryptionMode encryptionMode, EncryptionPaddingMode paddingMode,
			SignatureAlgorithm signatureAlgorithm) {
		byte[] decoded = Base64.getDecoder().decode(base64RawKey);
		return new Key(type, algorithm, decoded, ivSize, encryptionMode, paddingMode, signatureAlgorithm);
	}

	private KeySerializer() {
	}

}
