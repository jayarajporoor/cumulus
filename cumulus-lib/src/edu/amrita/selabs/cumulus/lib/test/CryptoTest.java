/*
 * This file is part of Cumulus software system developed at SE Labs, Amrita University.
 *
 * Cumulus is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * Cumulus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Libav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package edu.amrita.selabs.cumulus.lib.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import edu.amrita.selabs.cumulus.lib.StringUtil;


public class CryptoTest {

	@Before 
	public void setupBC()
	{
        Security.addProvider(new BouncyCastleProvider()); 
	}		
	
	@Test
	public void testBC() throws Exception
	{
		boolean foundBC = false;
         Provider p[] = Security.getProviders();
         for (int i = 0; i < p.length; i++) {
        	 if(p[i].getName().startsWith("BC"))
        	 {
        		 foundBC = true;
        	 }
             System.out.println(p[i]);
             for (Enumeration<?> e = p[i].keys(); e.hasMoreElements();)
                 System.out.println("\t" + e.nextElement());
         }
         assertTrue(foundBC);
	}
	
	@Test
	public void testAES256() throws Exception
	{
		final int keySize = 32;
		final int blockSize = 16;
		byte[] k = new byte[keySize];
		byte[] iv = new byte[blockSize];
		
		Arrays.fill(iv, (byte)0);
		
		for(int i=0;i< 32;i++)
			k[i] = (byte)i;
		String plainText = "Hello World";
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec key = new SecretKeySpec(k, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		byte[] cipherText = cipher.doFinal(plainText.getBytes());
		iv = cipher.getIV();
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		byte[] decText = cipher.doFinal(cipherText);
		assertTrue(new String(decText).equals(plainText));
		
		//System.out.println("IV: " + iv.length + " " + Util.toHexString(iv));
		
	}
	
	@Test	
	public void testCipherStream() throws Exception
	{
		final int keySize = 32;
		final int blockSize = 16;

		byte plainText[] = new byte[1001];
		Arrays.fill(plainText,  (byte)1);

		byte[] k = new byte[keySize];
		byte[] iv = new byte[blockSize];
		
		Arrays.fill(iv, (byte)0);
		
		for(int i=0;i< 32;i++)
			k[i] = (byte)i;
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec key = new SecretKeySpec(k, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));		

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		CipherOutputStream cOut = new CipherOutputStream(bOut, cipher);
		cOut.write(plainText);
		cOut.flush();
		cOut.close();		
		bOut.close();
		byte[] cipherText = bOut.toByteArray();
		
		byte[] decText = new byte[plainText.length];
		cipher.init(Cipher.DECRYPT_MODE,  key, new IvParameterSpec(iv));
		ByteArrayInputStream bIn = new ByteArrayInputStream(cipherText);
		CipherInputStream cIn = new CipherInputStream(bIn, cipher);
		int bs =0;
		int n = 0;
		final int delta = 50;//try to read some extra to see if there is anything extra
		do
		{
			bs = cIn.read(decText, n, plainText.length + delta - n);
			if(bs > 0)
				n += bs;
		}while( bs > 0);
		cIn.close();
		assertTrue(n == plainText.length);
		assertTrue(Arrays.equals(plainText, decText));
		
	}
	
	@Test
	
	public void testRSA() throws Exception
	{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.genKeyPair();
		Key publicKey = kp.getPublic();
		Key privateKey = kp.getPrivate();
		
		KeyFactory fact = KeyFactory.getInstance("RSA");
		RSAPublicKeySpec pub = fact.getKeySpec(publicKey, RSAPublicKeySpec.class);
		RSAPrivateKeySpec priv = fact.getKeySpec(privateKey,  RSAPrivateKeySpec.class);

		BigInteger modulus = pub.getModulus();
		BigInteger pubExp = pub.getPublicExponent();
		BigInteger privExp = priv.getPrivateExponent();
		
		String modulusStr = StringUtil.toHexString(modulus.toByteArray());
		String pubExpStr = StringUtil.toHexString(pubExp.toByteArray());
		String privExpStr = StringUtil.toHexString(privExp.toByteArray());
		
		System.out.println("modulus:" + modulusStr);
		System.out.println("pub ex:" + pubExpStr);
		System.out.println("priv ex:" + privExpStr);		
		
		BigInteger modulus1 = new BigInteger(StringUtil.toByteArray(modulusStr));
		BigInteger pubExp1 = new BigInteger(StringUtil.toByteArray(pubExpStr));
		BigInteger privExp1 = new BigInteger(StringUtil.toByteArray(privExpStr));
		
		RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus1, pubExp1);		
		RSAPublicKey pubKey = (RSAPublicKey) fact.generatePublic(pubKeySpec);
		RSAPublicKeySpec privKeySpec = new RSAPublicKeySpec(modulus1, privExp1);
		RSAPublicKey privKey = (RSAPublicKey) fact.generatePublic(privKeySpec);
		
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, privateKey);
			
		String text = "Aum Amriteswaryai Namah";

		byte[] cipherData = cipher.doFinal(text.getBytes());
		
		System.out.println("encData:" + StringUtil.toHexString(cipherData));		
			
		cipher.init(Cipher.DECRYPT_MODE,  pubKey);
		
		byte[] decData = cipher.doFinal(cipherData);
		
		System.out.println("decData:" + StringUtil.toHexString(decData));
		
		String decText = new String(decData, decData.length - text.length(), text.length());
		System.out.println("text bytes:" + StringUtil.toHexString(text.getBytes()));
		System.out.println("decText:" + decText);
		assertTrue(decText.equals(text));
	}


	@Test
	
	public void testRSAEncoded() throws Exception
	{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.genKeyPair();
		Key publicKey = kp.getPublic();
		Key privateKey = kp.getPrivate();
		
		KeyFactory fact = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec pub = fact.getKeySpec(publicKey,  X509EncodedKeySpec.class);
		PKCS8EncodedKeySpec priv = fact.getKeySpec(privateKey,  PKCS8EncodedKeySpec.class);		
		
		String pubString  =  new String(Base64.encode(pub.getEncoded()));
		String privString  = new String(Base64.encode(priv.getEncoded()));		
		
		System.out.println("public-key:" + pubString);
		System.out.println("private-key:" + privString);
		System.out.println("public key length: " + pubString.length());
		
		X509EncodedKeySpec pubNew = new X509EncodedKeySpec(Base64.decode(pubString));
		PKCS8EncodedKeySpec privNew = new PKCS8EncodedKeySpec(Base64.decode(privString));		
			
		RSAPublicKey publicKeyNew = (RSAPublicKey) fact.generatePublic(pubNew);
		RSAPrivateKey privateKeyNew = (RSAPrivateKey) fact.generatePrivate(privNew);
		
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, privateKey);
			
		String text = "Aum Amriteswaryai Namah";

		byte[] cipherData = cipher.doFinal(text.getBytes());
		
		System.out.println("encData:" + StringUtil.toHexString(cipherData));		
			
		cipher.init(Cipher.DECRYPT_MODE,  publicKeyNew);
		
		byte[] decData = cipher.doFinal(cipherData);
		
		System.out.println("decData:" + StringUtil.toHexString(decData));
		
		String decText = new String(decData, decData.length - text.length(), text.length());
		System.out.println("text bytes:" + StringUtil.toHexString(text.getBytes()));
		System.out.println("decText:" + decText);
		assertTrue(decText.equals(text));
	}
	
	@Test
	
	public void testSecureRandom() throws Exception
	{
		SecureRandom random = new SecureRandom();
	    byte bytes[] = new byte[20];
	    random.nextBytes(bytes);
	    System.out.println("Random:" + StringUtil.toHexString(bytes));
	}

}
