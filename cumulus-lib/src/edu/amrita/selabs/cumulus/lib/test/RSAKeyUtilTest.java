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

import java.security.Key;

import javax.crypto.Cipher;

import org.junit.Before;
import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.RSAKeyUtil;
import edu.amrita.selabs.cumulus.lib.StringUtil;

public class RSAKeyUtilTest {

	Key pubKey,pubKey2, privKey, privKey2;
	
	@Before
	public void setupKeys() throws Exception
	{
		RSAKeyUtil keyUtil = new RSAKeyUtil();
		keyUtil.genKeyPair();
		pubKey = keyUtil.getPublicKey();
		privKey = keyUtil.getPrivateKey();
		
		String sPub = keyUtil.getEncodedPublicKey();
		String sPriv = keyUtil.getEncodedPrivateKey();
		
		RSAKeyUtil keyUtil2 = new RSAKeyUtil();
		privKey2 = keyUtil2.decodePrivateKey(sPriv);
		pubKey2 = keyUtil2.decodePublicKey(sPub);
		
	}
	
	@Test
	public void testKeys() throws Exception{	
		assertTrue(pubKey.equals(pubKey2));
		assertTrue(privKey.equals(privKey2));		
	}
	
	@Test
	public void testSign() throws Exception
	{
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, privKey);
			
		String text = "Aum Amriteswaryai Namah";

		byte[] cipherData = cipher.doFinal(text.getBytes());
		
		System.out.println("encData:" + StringUtil.toHexString(cipherData));		
			
		cipher.init(Cipher.DECRYPT_MODE,  pubKey2);
		
		byte[] decData = cipher.doFinal(cipherData);
		
		System.out.println("decData:" + StringUtil.toHexString(decData));
		
		String decText = new String(decData, decData.length - text.length(), text.length());
		System.out.println("text bytes:" + StringUtil.toHexString(text.getBytes()));
		System.out.println("decText:" + decText);
		assertTrue(decText.equals(text));
		
	}

	@Test
	public void testEncrypt() throws Exception
	{
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, pubKey2);
			
		String text = "Aum Amriteswaryai Namah. Hello World!";

		byte[] cipherData = cipher.doFinal(text.getBytes());
		
		System.out.println("encData:" + StringUtil.toHexString(cipherData));		
			
		cipher.init(Cipher.DECRYPT_MODE,  privKey);
		
		byte[] decData = cipher.doFinal(cipherData);
		
		System.out.println("decData:" + StringUtil.toHexString(decData));
		
		String decText = new String(decData, decData.length - text.length(), text.length());
		System.out.println("text bytes:" + StringUtil.toHexString(text.getBytes()));
		System.out.println("decText:" + decText);
		assertTrue(decText.equals(text));
		
	}
	
}
