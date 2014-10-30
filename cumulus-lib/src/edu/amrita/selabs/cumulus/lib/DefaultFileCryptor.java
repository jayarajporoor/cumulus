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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DefaultFileCryptor implements FileCryptor{
	
	final int blockSize = 16;
	final int keySize = 32;
	byte[] buf = null;

	public void createReadBuf(int readBlockSize)
	{
		buf = new byte[readBlockSize];
	}
	
	public int getCipherBlockSize()
	{
		return blockSize;
	}

	public void encrypt(byte[] k, InputStream is, OutputStream os) throws IOException, 
															NoSuchAlgorithmException, NoSuchPaddingException,
															InvalidKeyException, InvalidAlgorithmParameterException
    {		
		crypt(k, is, os, true);
    }
	
	public void decrypt(byte[] k, InputStream is, OutputStream os) throws IOException, 
															NoSuchAlgorithmException, NoSuchPaddingException,
															InvalidKeyException, InvalidAlgorithmParameterException
	{
		crypt(k, is, os, false);
	}
	
	void crypt(byte[] k, InputStream is, OutputStream os, boolean isEncrypt) throws IOException, 
	                                                NoSuchAlgorithmException, NoSuchPaddingException,
                                                    InvalidKeyException, InvalidAlgorithmParameterException

	{
		if(buf == null)
			createReadBuf(1024);
		
		byte[] iv = new byte[blockSize];
		
		Arrays.fill(iv, (byte)0);
			
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec key = new SecretKeySpec(k, "AES");
		if(isEncrypt)
			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		else
			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

		CipherOutputStream cOut = new CipherOutputStream(os, cipher);
			
		int nRead = 0;
		do
		{
			nRead = is.read(buf);
			if(nRead > 0)
				cOut.write(buf, 0, nRead);
		}while(nRead > 0);
		
		cOut.close();
	}
	
	public byte[] sha256(InputStream is) throws IOException, NoSuchAlgorithmException
	{
		assert(buf != null);
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		
		DigestInputStream din = new DigestInputStream(is, sha256);
		
		int nRead = 0;
		do
		{
			nRead = din.read(buf);
		}while(nRead > 0);
		din.close();
		return sha256.digest();
		
	}
		
}
