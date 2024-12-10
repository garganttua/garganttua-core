package com.garganttua.keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

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

        if (input == null || !input.matches("^[A-Za-z0-9]+-[0-9]+$")) {
        	throw new IllegalArgumentException("Invalid format of "+input+", must be algo-size");
        }

        String[] parts = input.split("-");
        String algorithm = parts[0];
        int keySize;

        try {
            keySize = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid size "+parts[1]);
        }

        return Arrays.stream(GGKeyAlgorithm.values())
                .filter(ca -> ca.getAlgorithm().equalsIgnoreCase(algorithm) && ca.getKeySize() == keySize)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported size or algorithm "+input));
    }
    
    @Override
    public GGKeyRealmType getType() throws IllegalArgumentException {
        if (this.isSymetricAlgorithm()) {
            return GGKeyRealmType.SYMETRIC;
        } else if (this.isAsymetricAlgorithm()) {
            return GGKeyRealmType.ASYMETRIC;
        } else {
        	//Should never happen
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
        KeyGenerator keyGen;
		try {
			keyGen = KeyGenerator.getInstance(this.getAlgorithm());
			keyGen.init(this.keySize, GGKeyRandoms.secureRandom());
			return keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
    }

    @Override
    public KeyPair generateAsymetricKey() throws IllegalArgumentException {
        KeyPairGenerator keyGen;
		try {
			keyGen = KeyPairGenerator.getInstance(this.getAlgorithm());
			if (this.algorithm.equals("EC")) {
				if( this.keySize == 512 )
					keyGen.initialize(new ECGenParameterSpec("secp521r1"), GGKeyRandoms.secureRandom());
				else 
					keyGen.initialize(new ECGenParameterSpec("secp"+this.keySize+"r1"), GGKeyRandoms.secureRandom());
			} else {
				keyGen.initialize(this.keySize, GGKeyRandoms.secureRandom());
			}
			return keyGen.generateKeyPair();
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException  e) {
			throw new IllegalArgumentException(e);
		}
    }

    @Override
    public String geCipherName(GGEncryptionMode mode, GGEncryptionPaddingMode padding) throws IllegalArgumentException {
        if (mode == null || padding == null) {
            throw new IllegalArgumentException("Mode and Padding cannot be null");
        }
        
        return this.getAlgorithm() + "/" + mode + "/" + padding.getPadding();
    }

    @Override
	public String geSignatureName(GGSignatureAlgorithm signatureAlgorithm) {
		if (signatureAlgorithm == null) {
            throw new IllegalArgumentException("Signture algorithm cannot be null");
        }
        
        String algorithmName = this.algorithm;
        if( this.algorithm.equals("EC") )
        	algorithmName = "ECDSA";
        
        
		return signatureAlgorithm.getName()+"with"+algorithmName;
	}
}
