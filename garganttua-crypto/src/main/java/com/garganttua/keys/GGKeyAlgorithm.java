package com.garganttua.keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum GGKeyAlgorithm implements IGGKeyAlgorithm {
    // DSA
    DSA_512("DSA", 512),
    DSA_1024("DSA", 1024),
    DSA_2048("DSA", 2048),

    // RSA
    RSA_512("RSA", 512),
    RSA_1024("RSA", 1024),
    RSA_2048("RSA", 2048),
    RSA_3072("RSA", 3072),
    RSA_4096("RSA", 4096),

    // EC
    EC_256("EC", 256),
    EC_384("EC", 384),
    EC_512("EC", 512),

    // DH
    DH_512("DH", 512),
    DH_576("DH", 576),
    DH_640("DH", 640),
    DH_704("DH", 704),
    DH_768("DH", 768),
    DH_832("DH", 832),
    DH_896("DH", 896),
    DH_960("DH", 960),
    DH_1024("DH", 1024),
    DH_2048("DH", 2048),
    DH_3072("DH", 3072),
    DH_4096("DH", 4096),
    DH_8192("DH", 8192),

    // AES
    AES_128("AES", 128),
    AES_192("AES", 192),
    AES_256("AES", 256),

    // HmacSHA1
    HMAC_SHA1_128("HmacSHA1", 128),
    HMAC_SHA1_256("HmacSHA1", 256),
    HMAC_SHA1_384("HmacSHA1", 384),
    HMAC_SHA1_512("HmacSHA1", 512),
    HMAC_SHA1_1024("HmacSHA1", 1024),
    HMAC_SHA1_2048("HmacSHA1", 2048),
    HMAC_SHA1_4096("HmacSHA1", 4096),

    // HmacSHA224
    HMAC_SHA224_128("HmacSHA224", 128),
    HMAC_SHA224_256("HmacSHA224", 256),
    HMAC_SHA224_384("HmacSHA224", 384),
    HMAC_SHA224_512("HmacSHA224", 512),
    HMAC_SHA224_1024("HmacSHA224", 1024),
    HMAC_SHA224_2048("HmacSHA224", 2048),
    HMAC_SHA224_4096("HmacSHA224", 4096),

    // HmacSHA256
    HMAC_SHA256_128("HmacSHA256", 128),
    HMAC_SHA256_256("HmacSHA256", 256),
    HMAC_SHA256_384("HmacSHA256", 384),
    HMAC_SHA256_512("HmacSHA256", 512),
    HMAC_SHA256_1024("HmacSHA256", 1024),
    HMAC_SHA256_2048("HmacSHA256", 2048),
    HMAC_SHA256_4096("HmacSHA256", 4096),

    // HmacSHA384
    HMAC_SHA384_128("HmacSHA384", 128),
    HMAC_SHA384_256("HmacSHA384", 256),
    HMAC_SHA384_384("HmacSHA384", 384),
    HMAC_SHA384_512("HmacSHA384", 512),
    HMAC_SHA384_1024("HmacSHA384", 1024),
    HMAC_SHA384_2048("HmacSHA384", 2048),
    HMAC_SHA384_4096("HmacSHA384", 4096),

    // HmacSHA512
    HMAC_SHA512_128("HmacSHA512", 128),
    HMAC_SHA512_256("HmacSHA512", 256),
    HMAC_SHA512_384("HmacSHA512", 384),
    HMAC_SHA512_512("HmacSHA512", 512),
    HMAC_SHA512_1024("HmacSHA512", 1024),
    HMAC_SHA512_2048("HmacSHA512", 2048),
    HMAC_SHA512_4096("HmacSHA512", 4096),

    // ARCFOUR - RC4
    @Deprecated
    ARCFOUR_128("RC4", 128),
    @Deprecated
    ARCFOUR_256("RC4", 256),
    @Deprecated
    ARCFOUR_384("RC4", 384),
    @Deprecated
    ARCFOUR_512("RC4", 512),
    @Deprecated
    ARCFOUR_1024("RC4", 1024),

    // Blowfish
    BLOWFISH_32("Blowfish", 32),
    BLOWFISH_40("Blowfish", 40),
    BLOWFISH_48("Blowfish", 48),
    BLOWFISH_56("Blowfish", 56),
    BLOWFISH_64("Blowfish", 64),
    BLOWFISH_72("Blowfish", 72),
    BLOWFISH_80("Blowfish", 80),
    BLOWFISH_88("Blowfish", 88),
    BLOWFISH_96("Blowfish", 96),
    BLOWFISH_104("Blowfish", 104),
    BLOWFISH_112("Blowfish", 112),
    BLOWFISH_120("Blowfish", 120),
    BLOWFISH_128("Blowfish", 128),
    BLOWFISH_136("Blowfish", 136),
    BLOWFISH_144("Blowfish", 144),
    BLOWFISH_152("Blowfish", 152),
    BLOWFISH_160("Blowfish", 160),
    BLOWFISH_168("Blowfish", 168),
    BLOWFISH_176("Blowfish", 176),
    BLOWFISH_184("Blowfish", 184),
    BLOWFISH_192("Blowfish", 192),
    BLOWFISH_200("Blowfish", 200),
    BLOWFISH_208("Blowfish", 208),
    BLOWFISH_216("Blowfish", 216),
    BLOWFISH_224("Blowfish", 224),
    BLOWFISH_232("Blowfish", 232),
    BLOWFISH_240("Blowfish", 240),
    BLOWFISH_248("Blowfish", 248),
    BLOWFISH_256("Blowfish", 256),
    BLOWFISH_264("Blowfish", 264),
    BLOWFISH_272("Blowfish", 272),
    BLOWFISH_280("Blowfish", 280),
    BLOWFISH_288("Blowfish", 288),
    BLOWFISH_296("Blowfish", 296),
    BLOWFISH_304("Blowfish", 304),
    BLOWFISH_312("Blowfish", 312),
    BLOWFISH_320("Blowfish", 320),
    BLOWFISH_328("Blowfish", 328),
    BLOWFISH_336("Blowfish", 336),
    BLOWFISH_344("Blowfish", 344),
    BLOWFISH_352("Blowfish", 352),
    BLOWFISH_360("Blowfish", 360),
    BLOWFISH_368("Blowfish", 368),
    BLOWFISH_376("Blowfish", 376),
    BLOWFISH_384("Blowfish", 384),
    BLOWFISH_392("Blowfish", 392),
    BLOWFISH_400("Blowfish", 400),
    BLOWFISH_408("Blowfish", 408),
    BLOWFISH_416("Blowfish", 416),
    BLOWFISH_424("Blowfish", 424),
    BLOWFISH_432("Blowfish", 432),
    BLOWFISH_440("Blowfish", 440),
    BLOWFISH_448("Blowfish", 448),

    // DES
    DES_56("DES", 56),

    // DESede
    DESEDE_112("DESede", 112),
    DESEDE_168("DESede", 168),

    // RC2
    @Deprecated
    RC2_128("RC2", 128),
    @Deprecated
    RC2_256("RC2", 256),
    @Deprecated
    RC2_384("RC2", 384),
    @Deprecated
    RC2_512("RC2", 512),
    @Deprecated
    RC2_1024("RC2", 1024);

    private final String algorithm;
    private final int keySize;

    GGKeyAlgorithm(String algorithm, int keySize) {
        this.algorithm = algorithm;
        this.keySize = keySize;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getKeySize() {
        return keySize;
    }

    @Override
    public String toString() {
        return algorithm + "_" + keySize;
    }
    
    public static GGKeyAlgorithm validateKeyAlgorithm(String input) throws IllegalArgumentException {
        log.atTrace().log("Entering validateKeyAlgorithm with input: {}", input);

        if (input == null || !input.matches("^[A-Za-z0-9]+-[0-9]+$")) {
            log.atError().log("Invalid key algorithm format: {}", input);
        	throw new IllegalArgumentException("Invalid format of "+input+", must be algo-size");
        }

        String[] parts = input.split("-");
        String algorithm = parts[0];
        int keySize;

        try {
            keySize = Integer.parseInt(parts[1]);
            log.atDebug().log("Parsed key algorithm: {}, size: {}", algorithm, keySize);
        } catch (NumberFormatException e) {
            log.atError().log("Invalid key size: {}", parts[1]);
            throw new IllegalArgumentException("Invalid size "+parts[1]);
        }

        GGKeyAlgorithm result = Arrays.stream(GGKeyAlgorithm.values())
                .filter(ca -> ca.getAlgorithm().equalsIgnoreCase(algorithm) && ca.getKeySize() == keySize)
                .findFirst()
                .orElseThrow(() -> {
                    log.atError().log("Unsupported key algorithm or size: {}", input);
                    return new IllegalArgumentException("Unsupported size or algorithm "+input);
                });

        log.atDebug().log("Validated key algorithm: {}", result);
        log.atTrace().log("Exiting validateKeyAlgorithm");
        return result;
    }
    
    @Override
    public GGKeyRealmType getType() throws IllegalArgumentException {
        log.atTrace().log("Entering getType for algorithm: {}", this.algorithm);

        if (this.isSymetricAlgorithm()) {
            log.atDebug().log("Algorithm {} is symmetric", this.algorithm);
            log.atTrace().log("Exiting getType with SYMETRIC");
            return GGKeyRealmType.SYMETRIC;
        } else if (this.isAsymetricAlgorithm()) {
            log.atDebug().log("Algorithm {} is asymmetric", this.algorithm);
            log.atTrace().log("Exiting getType with ASYMETRIC");
            return GGKeyRealmType.ASYMETRIC;
        } else {
        	//Should never happen
            log.atError().log("Unsupported algorithm type: {}", this.algorithm);
        	throw new IllegalArgumentException("Unsupported algorithm "+this.algorithm);
        }
    }

    private boolean isSymetricAlgorithm() {
        switch (this.algorithm) {
            case "AES":
            case "HmacSHA1":
            case "HmacSHA224":
            case "HmacSHA256":
            case "HmacSHA384":
            case "HmacSHA512":
            case "RC4":
            case "Blowfish":
            case "DES":
            case "DESede":
            case "RC2":
                return true;
            default:
                return false;
        }
    }

    private boolean isAsymetricAlgorithm() {
        switch (this.algorithm) {
            case "DSA":
            case "RSA":
            case "RSASSA_PSS":
            case "EC":
            case "DH":
                return true;
            default:
                return false;
        }
    }

    @Override
    public SecretKey generateSymetricKey() throws IllegalArgumentException {
        log.atTrace().log("Entering generateSymetricKey for algorithm: {}, keySize: {}", this.algorithm, this.keySize);

        KeyGenerator keyGen;
		try {
			keyGen = KeyGenerator.getInstance(this.getAlgorithm());
			keyGen.init(this.keySize, GGKeyRandoms.secureRandom());
			log.atDebug().log("Generating symmetric key for algorithm: {}, size: {}", this.algorithm, this.keySize);
			SecretKey key = keyGen.generateKey();
			log.atDebug().log("Successfully generated symmetric key for {}", this.algorithm);
			log.atTrace().log("Exiting generateSymetricKey");
			return key;
		} catch (NoSuchAlgorithmException e) {
			log.atError().log("Failed to generate symmetric key for algorithm: {}", this.algorithm, e);
			throw new IllegalArgumentException(e);
		}
    }

    @Override
    public KeyPair generateAsymetricKey() throws IllegalArgumentException {
        log.atTrace().log("Entering generateAsymetricKey for algorithm: {}, keySize: {}", this.algorithm, this.keySize);

        KeyPairGenerator keyGen;
		try {
			keyGen = KeyPairGenerator.getInstance(this.getAlgorithm());
			if (this.algorithm.equals("EC")) {
				if( this.keySize == 512 ) {
					log.atDebug().log("Initializing EC key generator with secp521r1 curve");
					keyGen.initialize(new ECGenParameterSpec("secp521r1"), GGKeyRandoms.secureRandom());
				} else {
					log.atDebug().log("Initializing EC key generator with secp{}r1 curve", this.keySize);
					keyGen.initialize(new ECGenParameterSpec("secp"+this.keySize+"r1"), GGKeyRandoms.secureRandom());
				}
			} else {
				log.atDebug().log("Initializing key generator for algorithm: {}, size: {}", this.algorithm, this.keySize);
				keyGen.initialize(this.keySize, GGKeyRandoms.secureRandom());
			}
			log.atDebug().log("Generating asymmetric key pair for algorithm: {}", this.algorithm);
			KeyPair keyPair = keyGen.generateKeyPair();
			log.atDebug().log("Successfully generated asymmetric key pair for {}", this.algorithm);
			log.atTrace().log("Exiting generateAsymetricKey");
			return keyPair;
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException  e) {
			log.atError().log("Failed to generate asymmetric key for algorithm: {}", this.algorithm, e);
			throw new IllegalArgumentException(e);
		}
    }

    @Override
    public String geCipherName(GGEncryptionMode mode, GGEncryptionPaddingMode padding) throws IllegalArgumentException {
        log.atTrace().log("Entering geCipherName for algorithm: {}, mode: {}, padding: {}", this.algorithm, mode, padding);

        if (mode == null || padding == null) {
            log.atError().log("Mode or padding is null for algorithm: {}", this.algorithm);
            throw new IllegalArgumentException("Mode and Padding cannot be null");
        }

        String cipherName = this.getAlgorithm() + "/" + mode + "/" + padding.getPadding();
        log.atDebug().log("Generated cipher name: {}", cipherName);
        log.atTrace().log("Exiting geCipherName");
        return cipherName;
    }

    @Override
	public String geSignatureName(GGSignatureAlgorithm signatureAlgorithm) {
		log.atTrace().log("Entering geSignatureName for algorithm: {}, signatureAlgorithm: {}", this.algorithm, signatureAlgorithm);

		if (signatureAlgorithm == null) {
			log.atError().log("Signature algorithm is null for key algorithm: {}", this.algorithm);
            throw new IllegalArgumentException("Signture algorithm cannot be null");
        }

        String algorithmName = this.algorithm;
        if( this.algorithm.equals("EC") ) {
			log.atDebug().log("Converting EC to ECDSA for signature");
        	algorithmName = "ECDSA";
		}

		String signatureName = signatureAlgorithm.getName()+"with"+algorithmName;
		log.atDebug().log("Generated signature name: {}", signatureName);
		log.atTrace().log("Exiting geSignatureName");
		return signatureName;
	}
}
