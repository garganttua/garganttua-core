/**
 * Garganttua Cryptography Framework
 *
 * <p>This package provides a comprehensive cryptographic framework for secure key management,
 * encryption, decryption, and digital signatures. It supports both symmetric and asymmetric
 * cryptography with a wide range of algorithms, modes, and padding schemes.</p>
 *
 * <h2>Core Components</h2>
 *
 * <h3>KeyRealmBuilder</h3>
 * <p>Fluent builder for creating key realms with configurable algorithm, mode, padding, and signature.</p>
 *
 * <h3>IKey / Key</h3>
 * <p>Interface and implementation for cryptographic key operations including encryption, decryption,
 * signing, and signature verification.</p>
 *
 * <h2>Quick Start Examples</h2>
 *
 * <h3>Digital Signatures with RSA</h3>
 * <pre>{@code
 * IKeyRealm realm = KeyRealmBuilder.builder()
 *     .name("myRealm")
 *     .algorithm(KeyAlgorithm.RSA_4096)
 *     .signatureAlgorithm(SignatureAlgorithm.SHA224)
 *     .build();
 *
 * IKey signingKey = realm.getKeyForSigning();
 * IKey verifyingKey = realm.getKeyForSignatureVerification();
 *
 * byte[] signature = signingKey.sign("Salut".getBytes());
 * boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
 * }</pre>
 *
 * <h3>Symmetric Encryption (AES-256 with GCM)</h3>
 * <pre>{@code
 * IKeyRealm realm = KeyRealmBuilder.builder()
 *     .name("myRealm")
 *     .algorithm(KeyAlgorithm.AES_256)
 *     .initializationVectorSize(12)
 *     .encryptionMode(EncryptionMode.GCM)
 *     .paddingMode(EncryptionPaddingMode.NO_PADDING)
 *     .build();
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 * }</pre>
 *
 * <h3>Asymmetric Encryption (RSA)</h3>
 * <pre>{@code
 * IKeyRealm realm = KeyRealmBuilder.builder()
 *     .name("myRealm")
 *     .algorithm(KeyAlgorithm.RSA_4096)
 *     .encryptionMode(EncryptionMode.ECB)
 *     .paddingMode(EncryptionPaddingMode.PKCS1_PADDING)
 *     .build();
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.crypto.KeyRealmBuilder
 * @see com.garganttua.core.crypto.IKey
 * @see com.garganttua.core.crypto.KeyAlgorithm
 */
package com.garganttua.core.crypto;
