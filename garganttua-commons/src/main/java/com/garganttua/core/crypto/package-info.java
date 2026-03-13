/**
 * Cryptography interfaces and types for encryption, signing, hashing, and secure key management.
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.crypto.IKey} - Cryptographic key operations (encrypt, decrypt, sign, verify)</li>
 *   <li>{@link com.garganttua.core.crypto.IKeyRealm} - Key realm managing key lifecycle, rotation, and access</li>
 *   <li>{@link com.garganttua.core.crypto.IKeyAlgorithm} - Algorithm metadata (name, key size, type)</li>
 *   <li>{@link com.garganttua.core.crypto.IKeyRealmBuilder} - Fluent builder interface for key realm construction</li>
 *   <li>{@link com.garganttua.core.crypto.IHash} - Hashing operations (hash, verify)</li>
 * </ul>
 *
 * <h2>Enums</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.crypto.KeyType} - PUBLIC, PRIVATE, SECRET</li>
 *   <li>{@link com.garganttua.core.crypto.KeyAlgorithmType} - SYMMETRIC, ASYMMETRIC</li>
 *   <li>{@link com.garganttua.core.crypto.EncryptionMode} - ECB, CBC, CFB, OFB, GCM, CTR</li>
 *   <li>{@link com.garganttua.core.crypto.EncryptionPaddingMode} - NoPadding, PKCS5, PKCS1, etc.</li>
 *   <li>{@link com.garganttua.core.crypto.SignatureAlgorithm} - SHA family, MD family, BLAKE2, etc.</li>
 *   <li>{@link com.garganttua.core.crypto.HashAlgorithm} - SHA-256, SHA-512, SHA3-256, etc.</li>
 * </ul>
 *
 * <h2>Exception</h2>
 * <p>{@link com.garganttua.core.crypto.CryptoException} extends {@link com.garganttua.core.CoreException}
 * with error code {@link com.garganttua.core.CoreException#CRYPTO_ERROR}.</p>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.crypto;
