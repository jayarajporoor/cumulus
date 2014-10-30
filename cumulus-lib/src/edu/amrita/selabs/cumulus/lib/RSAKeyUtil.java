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

package edu.amrita.selabs.cumulus.lib;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.util.encoders.Base64;

public class RSAKeyUtil {
	
	private KeyFactory fact = null;
	private PublicKey pubKey = null;
	private PrivateKey privKey =null;

	public void genKeyPair() throws NoSuchAlgorithmException
	{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.genKeyPair();
		pubKey = kp.getPublic();
		privKey = kp.getPrivate();
	}
	
	public String getEncodedPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		initKeyFactory();

		PKCS8EncodedKeySpec priv = fact.getKeySpec(privKey,  PKCS8EncodedKeySpec.class);		
		
		String s = new String(Base64.encode(priv.getEncoded()));		
		
		return s;
	}
	
	public String getEncodedPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		initKeyFactory();
		X509EncodedKeySpec pub = fact.getKeySpec(pubKey,  X509EncodedKeySpec.class);	
		
		String s  =  new String(Base64.encode(pub.getEncoded()));
		return s;		
	}
	
	public PublicKey decodePublicKey(String s) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		initKeyFactory();		
		X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.decode(s));
		
		pubKey =  fact.generatePublic(pubSpec);
		return pubKey;
	}
	
	public PrivateKey decodePrivateKey(String s) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		initKeyFactory();
		PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(Base64.decode(s));		
			
		privKey = fact.generatePrivate(privSpec);
		
		return privKey;		
	}
	
	public PrivateKey getPrivateKey()
	{
		return privKey;
	}
	
	public PublicKey getPublicKey()
	{
		return pubKey;
	}
	
	private void initKeyFactory() throws NoSuchAlgorithmException 
	{
		if(fact == null) 
			fact = KeyFactory.getInstance("RSA");
	}
	
	
}
