package edu.amrita.selabs.cumulus.lib;

import java.security.Key;
import java.security.MessageDigest;

import javax.crypto.Cipher;

import org.bouncycastle.util.Arrays;

public class RSACryptoUtil {
	
	int sha256BlockSize = 256/8;
	
	public String sign(String data, Key k) throws Exception
	{	
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		sha256.update(data.getBytes());
		byte[] hash = sha256.digest();
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, k);
			
		byte[] sign = cipher.doFinal(hash);
		return StringUtil.toHexString(sign);
	}

	public boolean verify(String data, String sign, Key k) throws Exception
	{
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		sha256.update(data.getBytes());
		byte[] hash = sha256.digest();
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		byte[] signedHash = StringUtil.toByteArray(sign);
		cipher.init(Cipher.DECRYPT_MODE, k);
			
		byte[] decrypted = cipher.doFinal(signedHash);
		
		byte[] rhash = new byte[sha256BlockSize];
		for(int i =0,j=decrypted.length-rhash.length;i<rhash.length;i++,j++)
		{
			rhash[i] = decrypted[j];
		}
		return Arrays.areEqual(rhash, hash);
	}
	
}
