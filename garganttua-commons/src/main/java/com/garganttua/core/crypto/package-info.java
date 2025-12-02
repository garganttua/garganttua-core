/**
 * Cryptography utilities for encryption, hashing, and secure key management.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides cryptographic operations including symmetric/asymmetric encryption,
 * secure hashing, key generation, and key management. It offers high-level APIs built on
 * Java's standard cryptography libraries.
 * </p>
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>IEncryption</b> - Encryption/decryption operations (provided by implementations)</li>
 *   <li><b>IHash</b> - Secure hashing algorithms (provided by implementations)</li>
 *   <li><b>IKeyManager</b> - Key generation and storage (provided by implementations)</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Symmetric Encryption</b> - AES-256, ChaCha20</li>
 *   <li><b>Asymmetric Encryption</b> - RSA, ECC</li>
 *   <li><b>Hashing</b> - SHA-256, SHA-512, BLAKE2</li>
 *   <li><b>Password Hashing</b> - bcrypt, scrypt, Argon2</li>
 *   <li><b>Key Management</b> - Secure key generation and storage</li>
 *   <li><b>Digital Signatures</b> - RSA-PSS, ECDSA</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Symmetric Encryption</h3>
 * <pre>{@code
 * // Generate key
 * SecretKey key = KeyGenerator.getInstance("AES").generateKey();
 *
 * // Encrypt
 * IEncryption encryption = new AESEncryption(key);
 * byte[] plaintext = "Secret message".getBytes(StandardCharsets.UTF_8);
 * byte[] ciphertext = encryption.encrypt(plaintext);
 *
 * // Decrypt
 * byte[] decrypted = encryption.decrypt(ciphertext);
 * String message = new String(decrypted, StandardCharsets.UTF_8);
 * }</pre>
 *
 * <h3>Secure Hashing</h3>
 * <pre>{@code
 * IHash hash = new SHA256Hash();
 * byte[] data = "Important document".getBytes(StandardCharsets.UTF_8);
 * byte[] digest = hash.hash(data);
 *
 * // Verify integrity
 * boolean isValid = hash.verify(data, digest);
 * }</pre>
 *
 * <h3>Password Hashing</h3>
 * <pre>{@code
 * IPasswordHash passwordHash = new BcryptPasswordHash();
 *
 * // Hash password during registration
 * String password = "user-password";
 * String hashed = passwordHash.hash(password);
 * // Store hashed in database
 *
 * // Verify password during login
 * String inputPassword = "user-password";
 * boolean isValid = passwordHash.verify(inputPassword, hashed);
 * }</pre>
 *
 * <h3>Digital Signatures</h3>
 * <pre>{@code
 * // Generate key pair
 * KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
 * keyGen.initialize(2048);
 * KeyPair keyPair = keyGen.generateKeyPair();
 *
 * // Sign document
 * IDigitalSignature signature = new RSASignature(keyPair.getPrivate());
 * byte[] document = "Contract terms...".getBytes(StandardCharsets.UTF_8);
 * byte[] sign = signature.sign(document);
 *
 * // Verify signature
 * IDigitalSignature verifier = new RSASignature(keyPair.getPublic());
 * boolean isAuthentic = verifier.verify(document, sign);
 * }</pre>
 *
 * <h2>Security Best Practices</h2>
 * <ul>
 *   <li>Always use strong key sizes (AES-256, RSA-2048+)</li>
 *   <li>Never hard-code cryptographic keys</li>
 *   <li>Use secure random number generators</li>
 *   <li>Rotate keys periodically</li>
 *   <li>Store keys in secure key stores (HSM, KMS)</li>
 *   <li>Use authenticated encryption (GCM mode)</li>
 * </ul>
 *
 * <h2>Algorithms Supported</h2>
 * <ul>
 *   <li><b>Symmetric</b>: AES-128/192/256-GCM, ChaCha20-Poly1305</li>
 *   <li><b>Asymmetric</b>: RSA-2048/4096, EC P-256/P-384/P-521</li>
 *   <li><b>Hashing</b>: SHA-256, SHA-384, SHA-512, BLAKE2b</li>
 *   <li><b>Password</b>: bcrypt (cost 12), scrypt, Argon2id</li>
 *   <li><b>Signature</b>: RSA-PSS, ECDSA, EdDSA</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.crypto;
