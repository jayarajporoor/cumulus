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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.DefaultFileCryptor;
import edu.amrita.selabs.cumulus.lib.StringUtil;

public class DefaultFileEncryptorTest {

	@Test
	public void testEncrypt() throws Exception{
		DefaultFileCryptor encryptor = new DefaultFileCryptor();
		
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

		ByteArrayInputStream  bIn = new ByteArrayInputStream(plainText);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		encryptor.createReadBuf(256);
		encryptor.encrypt(k,  bIn, bOut);
		bIn.close();
		bOut.close();
		
		byte[] cipherText = bOut.toByteArray();
		
		byte[] decText = new byte[plainText.length];
		cipher.init(Cipher.DECRYPT_MODE,  key, new IvParameterSpec(iv));
		bIn = new ByteArrayInputStream(cipherText);
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
	public void testSHA256() throws Exception
	{
		String msg = "Aum Amriteswaryai Namah";
		byte[] b = msg.getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(b);
		DefaultFileCryptor encryptor = new DefaultFileCryptor();
		encryptor.createReadBuf(50);
		byte[] sha256 = encryptor.sha256(is);
		is.close();
		String hash = StringUtil.toHexString(sha256);

		assertTrue(hash.equalsIgnoreCase("560859d74c4021e97039eade1931ee3aca1a5c87b041e87a5564badc2a2747e6"));
	}

}
