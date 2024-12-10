package com.garganttua.api.core.security.key;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.Test;

public class TestKeys {

	@Test
	public void testAsymetricKeys() throws NoSuchAlgorithmException {
		KeyPairGenerator.getInstance("DSA").initialize(512);
		KeyPairGenerator.getInstance("DSA").initialize(1024);
		KeyPairGenerator.getInstance("DSA").initialize(2048);

		KeyPairGenerator.getInstance("RSA").initialize(512);
		KeyPairGenerator.getInstance("RSA").initialize(1024);
		KeyPairGenerator.getInstance("RSA").initialize(2048);
		KeyPairGenerator.getInstance("RSA").initialize(4096);

		KeyPairGenerator.getInstance("RSASSA-PSS").initialize(512);
		KeyPairGenerator.getInstance("RSASSA-PSS").initialize(1024);
		KeyPairGenerator.getInstance("RSASSA-PSS").initialize(2048);
		KeyPairGenerator.getInstance("RSASSA-PSS").initialize(4096);

		KeyPairGenerator.getInstance("EC").initialize(128);
		KeyPairGenerator.getInstance("EC").initialize(256);
		KeyPairGenerator.getInstance("EC").initialize(384);
		KeyPairGenerator.getInstance("EC").initialize(512);
		
		KeyPairGenerator.getInstance("DH").initialize(512);
		KeyPairGenerator.getInstance("DH").initialize(576);
		KeyPairGenerator.getInstance("DH").initialize(640);
		KeyPairGenerator.getInstance("DH").initialize(704);
		KeyPairGenerator.getInstance("DH").initialize(768);
		KeyPairGenerator.getInstance("DH").initialize(832);
		KeyPairGenerator.getInstance("DH").initialize(896);
		KeyPairGenerator.getInstance("DH").initialize(960);
		KeyPairGenerator.getInstance("DH").initialize(1024);
		KeyPairGenerator.getInstance("DH").initialize(3072);
		KeyPairGenerator.getInstance("DH").initialize(2048);
		KeyPairGenerator.getInstance("DH").initialize(4096);
		KeyPairGenerator.getInstance("DH").initialize(8192);

		
	}
	
	@Test
	public void testSymetricKeys() throws NoSuchAlgorithmException {
		KeyGenerator.getInstance("AES").init(128);
		KeyGenerator.getInstance("AES").init(192);
		KeyGenerator.getInstance("AES").init(256);
		
		KeyGenerator.getInstance("HmacSHA1").init(128);
		KeyGenerator.getInstance("HmacSHA1").init(256);
		KeyGenerator.getInstance("HmacSHA1").init(384);
		KeyGenerator.getInstance("HmacSHA1").init(512);
		KeyGenerator.getInstance("HmacSHA1").init(1024);
		KeyGenerator.getInstance("HmacSHA1").init(2048);
		KeyGenerator.getInstance("HmacSHA1").init(4096);
		
		KeyGenerator.getInstance("HmacSHA224").init(128);
		KeyGenerator.getInstance("HmacSHA224").init(256);
		KeyGenerator.getInstance("HmacSHA224").init(384);
		KeyGenerator.getInstance("HmacSHA224").init(512);
		KeyGenerator.getInstance("HmacSHA224").init(1024);
		KeyGenerator.getInstance("HmacSHA224").init(2048);
		KeyGenerator.getInstance("HmacSHA224").init(4096);

		KeyGenerator.getInstance("HmacSHA256").init(128);
		KeyGenerator.getInstance("HmacSHA256").init(256);
		KeyGenerator.getInstance("HmacSHA256").init(384);
		KeyGenerator.getInstance("HmacSHA256").init(512);
		KeyGenerator.getInstance("HmacSHA256").init(1024);
		KeyGenerator.getInstance("HmacSHA256").init(2048);
		KeyGenerator.getInstance("HmacSHA256").init(4096);
		
		KeyGenerator.getInstance("HmacSHA384").init(128);
		KeyGenerator.getInstance("HmacSHA384").init(256);
		KeyGenerator.getInstance("HmacSHA384").init(384);
		KeyGenerator.getInstance("HmacSHA384").init(512);
		KeyGenerator.getInstance("HmacSHA384").init(1024);
		KeyGenerator.getInstance("HmacSHA384").init(2048);
		KeyGenerator.getInstance("HmacSHA384").init(4096);
		
		KeyGenerator.getInstance("HmacSHA512").init(128);
		KeyGenerator.getInstance("HmacSHA512").init(256);
		KeyGenerator.getInstance("HmacSHA512").init(384);
		KeyGenerator.getInstance("HmacSHA512").init(512);
		KeyGenerator.getInstance("HmacSHA512").init(1024);
		KeyGenerator.getInstance("HmacSHA512").init(2048);
		KeyGenerator.getInstance("HmacSHA512").init(4096);

		KeyGenerator.getInstance("ARCFOUR").init(128);
		KeyGenerator.getInstance("ARCFOUR").init(256);
		KeyGenerator.getInstance("ARCFOUR").init(384);
		KeyGenerator.getInstance("ARCFOUR").init(512);
		KeyGenerator.getInstance("ARCFOUR").init(1024);
		
		KeyGenerator.getInstance("Blowfish").init(32);
		KeyGenerator.getInstance("Blowfish").init(40);
		KeyGenerator.getInstance("Blowfish").init(48);
		KeyGenerator.getInstance("Blowfish").init(56);
		KeyGenerator.getInstance("Blowfish").init(64);
		KeyGenerator.getInstance("Blowfish").init(72);
		KeyGenerator.getInstance("Blowfish").init(80);
		KeyGenerator.getInstance("Blowfish").init(88);
		KeyGenerator.getInstance("Blowfish").init(96);
		KeyGenerator.getInstance("Blowfish").init(104);
		KeyGenerator.getInstance("Blowfish").init(112);
		KeyGenerator.getInstance("Blowfish").init(120);
		KeyGenerator.getInstance("Blowfish").init(128);
		KeyGenerator.getInstance("Blowfish").init(136);
		KeyGenerator.getInstance("Blowfish").init(144);
		KeyGenerator.getInstance("Blowfish").init(152);
		KeyGenerator.getInstance("Blowfish").init(160);
		KeyGenerator.getInstance("Blowfish").init(168);
		KeyGenerator.getInstance("Blowfish").init(176);
		KeyGenerator.getInstance("Blowfish").init(184);
		KeyGenerator.getInstance("Blowfish").init(192);
		KeyGenerator.getInstance("Blowfish").init(200);
		KeyGenerator.getInstance("Blowfish").init(208);
		KeyGenerator.getInstance("Blowfish").init(216);
		KeyGenerator.getInstance("Blowfish").init(224);
		KeyGenerator.getInstance("Blowfish").init(232);
		KeyGenerator.getInstance("Blowfish").init(240);
		KeyGenerator.getInstance("Blowfish").init(248);
		KeyGenerator.getInstance("Blowfish").init(256);
		KeyGenerator.getInstance("Blowfish").init(264);
		KeyGenerator.getInstance("Blowfish").init(272);
		KeyGenerator.getInstance("Blowfish").init(280);
		KeyGenerator.getInstance("Blowfish").init(288);
		KeyGenerator.getInstance("Blowfish").init(296);
		KeyGenerator.getInstance("Blowfish").init(304);
		KeyGenerator.getInstance("Blowfish").init(312);
		KeyGenerator.getInstance("Blowfish").init(320);
		KeyGenerator.getInstance("Blowfish").init(328);
		KeyGenerator.getInstance("Blowfish").init(336);
		KeyGenerator.getInstance("Blowfish").init(344);
		KeyGenerator.getInstance("Blowfish").init(352);
		KeyGenerator.getInstance("Blowfish").init(360);
		KeyGenerator.getInstance("Blowfish").init(368);
		KeyGenerator.getInstance("Blowfish").init(376);
		KeyGenerator.getInstance("Blowfish").init(384);
		KeyGenerator.getInstance("Blowfish").init(392);
		KeyGenerator.getInstance("Blowfish").init(400);
		KeyGenerator.getInstance("Blowfish").init(408);
		KeyGenerator.getInstance("Blowfish").init(416);
		KeyGenerator.getInstance("Blowfish").init(424);
		KeyGenerator.getInstance("Blowfish").init(432);
		KeyGenerator.getInstance("Blowfish").init(440);
		KeyGenerator.getInstance("Blowfish").init(448);
		
		KeyGenerator.getInstance("DES");
		
		KeyGenerator.getInstance("DESede").init(112);
		KeyGenerator.getInstance("DESede").init(168);
		
		KeyGenerator.getInstance("HmacMD5");

		KeyGenerator.getInstance("RC2").init(128);
		KeyGenerator.getInstance("RC2").init(256);
		KeyGenerator.getInstance("RC2").init(384);
		KeyGenerator.getInstance("RC2").init(512);
		KeyGenerator.getInstance("RC2").init(1024);
	}
	
	
	
}
