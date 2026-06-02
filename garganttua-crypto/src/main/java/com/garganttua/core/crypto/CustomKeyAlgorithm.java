package com.garganttua.core.crypto;

/**
 * User-defined {@link IKeyAlgorithm} for algorithms not covered by the built-in
 * {@link KeyAlgorithm} enum. Holds an algorithm name, key size and
 * {@link KeyAlgorithmType}, and derives JCA cipher and signature names the same
 * way the enum does.
 */
public class CustomKeyAlgorithm implements IKeyAlgorithm {

	private final String name;

	private final int keySize;

	private final KeyAlgorithmType type;

	/**
	 * Creates a custom algorithm descriptor.
	 *
	 * @param name    JCA algorithm name (e.g. {@code "AES"})
	 * @param keySize key size in bits
	 * @param type    whether the algorithm is symmetric or asymmetric
	 */
	public CustomKeyAlgorithm(String name, int keySize, KeyAlgorithmType type) {
		this.name = name;
		this.keySize = keySize;
		this.type = type;
	}

	/**
	 * Builds the JCA transformation string {@code name/mode/padding}.
	 *
	 * @param mode    encryption mode (block-cipher mode of operation)
	 * @param padding padding scheme
	 * @return the transformation string usable with {@code Cipher.getInstance}
	 * @throws IllegalArgumentException if {@code mode} or {@code padding} is {@code null}
	 */
	@Override
	public String getCipherName(EncryptionMode mode, EncryptionPaddingMode padding) {
		if (mode == null || padding == null) {
			throw new IllegalArgumentException("Mode and Padding cannot be null");
		}
		return name + "/" + mode + "/" + padding.getPadding();
	}

	/**
	 * Builds the JCA signature name {@code <digest>with<algorithm>}.
	 *
	 * @param signatureAlgorithm the digest portion of the signature name
	 * @return the signature name usable with {@code Signature.getInstance}
	 * @throws IllegalArgumentException if {@code signatureAlgorithm} is {@code null}
	 */
	@Override
	public String getSignatureName(SignatureAlgorithm signatureAlgorithm) {
		if (signatureAlgorithm == null) {
			throw new IllegalArgumentException("Signature algorithm cannot be null");
		}
		return signatureAlgorithm.getName() + "with" + name;
	}

	/** {@return the JCA algorithm name} */
	@Override
	public String getName() {
		return this.name;
	}

	/** {@return the key size in bits} */
	@Override
	public int getKeySize() {
		return this.keySize;
	}

	/** {@return whether the algorithm is symmetric or asymmetric} */
	@Override
	public KeyAlgorithmType getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return name + "_" + keySize;
	}

}
