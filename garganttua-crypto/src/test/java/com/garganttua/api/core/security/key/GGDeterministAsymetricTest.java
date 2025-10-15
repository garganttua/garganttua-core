package com.garganttua.api.core.security.key;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.Test;

public class GGDeterministAsymetricTest {
	
	@Test
	public void testDeterminist() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        // Générer une paire de clés RSA
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // Message à chiffrer
        String message = "Message déterministe";

        // Étape 1 : Hachage
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedMessage = digest.digest(message.getBytes());

        // Étape 2 : Chiffrement
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        byte[] encryptedHash = cipher.doFinal(hashedMessage);
        byte[] encryptedHash2 = cipher.doFinal(hashedMessage);
        byte[] encryptedHash3 = cipher.doFinal(hashedMessage);
        
        byte[] b64EncryptedHash = Base64.getEncoder().encode(encryptedHash);
        byte[] b64EncryptedHash2 = Base64.getEncoder().encode(encryptedHash2);
        byte[] b64EncryptedHash3 = Base64.getEncoder().encode(encryptedHash3);

        System.out.println("Message chiffré b64 : " + new String(b64EncryptedHash));
        System.out.println("Message chiffré b64 2 : " + new String(b64EncryptedHash2));
        System.out.println("Message chiffré b64 3 : " + new String(b64EncryptedHash3));
    }

}
