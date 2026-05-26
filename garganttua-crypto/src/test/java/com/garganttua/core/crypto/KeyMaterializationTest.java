package com.garganttua.core.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates the {@link Key#fromSigningMaterial}, {@link Key#fromEncryptionMaterial},
 * {@link KeyRealm#fromSignatureMaterial} and {@link KeyRealm#fromEncryptionMaterial}
 * factories, plus the lazy JDK key cache introduced in {@link Key#getKey()}.
 */
@DisplayName("Key materialization + JDK key cache tests")
class KeyMaterializationTest {

    private static byte[] decode(byte[] base64) {
        return Base64.getDecoder().decode(base64);
    }

    // ---------- Key.fromSigningMaterial ----------

    @Test
    @DisplayName("Round-trip: sign with reconstructed key, verify with original public key")
    void rsaSigningMaterial_signByReloaded_verifyByOrigin() throws Exception {
        IKeyRealm origin = KeyRealmBuilder.builder()
                .name("round-trip")
                .algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256)
                .build();

        byte[] privBytes = decode(origin.getKeyForSigning().getRawKey());
        byte[] pubBytes = decode(origin.getKeyForSignatureVerification().getRawKey());

        Key reloadedPrivate = Key.fromSigningMaterial(KeyType.PRIVATE,
                KeyAlgorithm.RSA_2048, SignatureAlgorithm.SHA256, privBytes);
        Key reloadedPublic = Key.fromSigningMaterial(KeyType.PUBLIC,
                KeyAlgorithm.RSA_2048, SignatureAlgorithm.SHA256, pubBytes);

        byte[] payload = "round-trip-payload".getBytes();

        // Reloaded private signs, original public verifies
        byte[] sig1 = reloadedPrivate.sign(payload);
        assertTrue(origin.getKeyForSignatureVerification().verifySignature(sig1, payload));

        // Original private signs, reloaded public verifies
        byte[] sig2 = origin.getKeyForSigning().sign(payload);
        assertTrue(reloadedPublic.verifySignature(sig2, payload));
    }

    @Test
    @DisplayName("EC signing material round-trips")
    void ecSigningMaterial_roundTrips() throws Exception {
        IKeyRealm origin = KeyRealmBuilder.builder()
                .name("ec-round-trip")
                .algorithm(KeyAlgorithm.EC_256)
                .signatureAlgorithm(SignatureAlgorithm.SHA256)
                .build();

        byte[] privBytes = decode(origin.getKeyForSigning().getRawKey());
        byte[] pubBytes = decode(origin.getKeyForSignatureVerification().getRawKey());

        Key priv = Key.fromSigningMaterial(KeyType.PRIVATE,
                KeyAlgorithm.EC_256, SignatureAlgorithm.SHA256, privBytes);
        Key pub = Key.fromSigningMaterial(KeyType.PUBLIC,
                KeyAlgorithm.EC_256, SignatureAlgorithm.SHA256, pubBytes);

        byte[] data = "ec-payload".getBytes();
        byte[] sig = priv.sign(data);
        assertTrue(pub.verifySignature(sig, data));
    }

    @Test
    @DisplayName("fromSigningMaterial rejects SECRET keys")
    void fromSigningMaterial_rejectsSecret() {
        assertThrows(IllegalArgumentException.class,
                () -> Key.fromSigningMaterial(KeyType.SECRET, KeyAlgorithm.RSA_2048,
                        SignatureAlgorithm.SHA256, new byte[16]));
    }

    @Test
    @DisplayName("Null arguments throw NPE with the field name")
    void nullArgs_throwNpe() {
        assertThrows(NullPointerException.class,
                () -> Key.fromSigningMaterial(null, KeyAlgorithm.RSA_2048,
                        SignatureAlgorithm.SHA256, new byte[1]));
        assertThrows(NullPointerException.class,
                () -> Key.fromSigningMaterial(KeyType.PRIVATE, null,
                        SignatureAlgorithm.SHA256, new byte[1]));
        assertThrows(NullPointerException.class,
                () -> Key.fromSigningMaterial(KeyType.PRIVATE, KeyAlgorithm.RSA_2048,
                        null, new byte[1]));
        assertThrows(NullPointerException.class,
                () -> Key.fromSigningMaterial(KeyType.PRIVATE, KeyAlgorithm.RSA_2048,
                        SignatureAlgorithm.SHA256, null));
    }

    // ---------- KeyRealm.fromSignatureMaterial ----------

    @Test
    @DisplayName("Realm rebuilt from material wraps the exact bytes — no regeneration")
    void realmFromSignatureMaterial_noRegeneration() throws Exception {
        IKeyRealm origin = KeyRealmBuilder.builder()
                .name("no-regen")
                .algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256)
                .build();

        byte[] privB64 = origin.getKeyForSigning().getRawKey();
        byte[] pubB64 = origin.getKeyForSignatureVerification().getRawKey();

        KeyRealm reloaded = KeyRealm.fromSignatureMaterial(
                "no-regen", KeyAlgorithm.RSA_2048, SignatureAlgorithm.SHA256,
                null, false, decode(privB64), decode(pubB64));

        // Same Base64-encoded raw bytes (i.e. wrapping, not regeneration).
        assertArrayEquals(privB64, reloaded.getKeyForSigning().getRawKey());
        assertArrayEquals(pubB64, reloaded.getKeyForSignatureVerification().getRawKey());

        // And sign/verify cross-realm works.
        byte[] data = "cross-realm".getBytes();
        byte[] sig = reloaded.getKeyForSigning().sign(data);
        assertTrue(origin.getKeyForSignatureVerification().verifySignature(sig, data));
    }

    @Test
    @DisplayName("Revoked flag propagates from material to realm")
    void revokedFlagPropagates() throws Exception {
        IKeyRealm origin = KeyRealmBuilder.builder()
                .name("revoked")
                .algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256)
                .build();
        byte[] privBytes = decode(origin.getKeyForSigning().getRawKey());
        byte[] pubBytes = decode(origin.getKeyForSignatureVerification().getRawKey());

        KeyRealm reloaded = KeyRealm.fromSignatureMaterial(
                "revoked", KeyAlgorithm.RSA_2048, SignatureAlgorithm.SHA256,
                null, true, privBytes, pubBytes);

        assertTrue(reloaded.isRevoked());
        // Using a revoked realm throws.
        assertThrows(CryptoException.class, reloaded::getKeyForSigning);
    }

    @Test
    @DisplayName("Backward compat: KeyRealmBuilder.build() still generates a fresh pair")
    void backwardCompatGeneration() throws Exception {
        IKeyRealm a = KeyRealmBuilder.builder()
                .name("a").algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256).build();
        IKeyRealm b = KeyRealmBuilder.builder()
                .name("b").algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256).build();
        // Two generated realms have distinct key material.
        assertFalse(java.util.Arrays.equals(
                a.getKeyForSigning().getRawKey(),
                b.getKeyForSigning().getRawKey()));
    }

    // ---------- JDK key cache ----------

    @Test
    @DisplayName("getKey() returns the same JDK instance on repeat calls")
    void cache_singleInstance() throws Exception {
        IKeyRealm realm = KeyRealmBuilder.builder()
                .name("cache").algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256).build();
        IKey k = realm.getKeyForSigning();

        java.security.Key first = k.getKey();
        java.security.Key second = k.getKey();
        assertSame(first, second, "cached JDK key must be the same instance");
        assertNotNull(first);
    }

    @Test
    @DisplayName("Cache is thread-safe: 10 threads × 1000 calls yield ONE JDK instance")
    void cache_threadSafety() throws Exception {
        IKeyRealm realm = KeyRealmBuilder.builder()
                .name("threads").algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256).build();
        IKey k = realm.getKeyForSigning();

        int threads = 10;
        int loops = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        Set<java.security.Key> seen = ConcurrentHashMap.newKeySet();
        AtomicInteger errors = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < loops; i++) {
                        seen.add(k.getKey());
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "executor must finish");
        assertEquals(0, errors.get(), "no thread should have raised an exception");
        assertEquals(1, seen.size(),
                "all threads must observe the SAME cached JDK key instance, but got: " + seen.size());
    }

    @Test
    @DisplayName("Equality remains byte-content based even after cache population")
    void equals_byteContentBased_postCache() throws Exception {
        IKeyRealm origin = KeyRealmBuilder.builder()
                .name("eq").algorithm(KeyAlgorithm.RSA_2048)
                .signatureAlgorithm(SignatureAlgorithm.SHA256).build();
        byte[] privBytes = decode(origin.getKeyForSigning().getRawKey());

        Key k1 = Key.fromSigningMaterial(KeyType.PRIVATE,
                KeyAlgorithm.RSA_2048, SignatureAlgorithm.SHA256, privBytes);
        Key k2 = Key.fromSigningMaterial(KeyType.PRIVATE,
                KeyAlgorithm.RSA_2048, SignatureAlgorithm.SHA256, privBytes);

        // Pre-cache equality.
        assertEquals(k1, k2);
        assertEquals(k1.hashCode(), k2.hashCode());

        // Populate k1's cache; equality must still hold (cache reference is not
        // part of identity).
        k1.getKey();
        assertEquals(k1, k2, "equals must not depend on cache state");
        assertEquals(k1.hashCode(), k2.hashCode(), "hashCode must not depend on cache state");
    }
}
