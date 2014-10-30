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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

public interface FileCryptor {

	public void createReadBuf(int readBlockSize);
	
	public int getCipherBlockSize();
	
	public void encrypt(byte[] key, InputStream is, OutputStream os) throws IOException, 
	                                                  NoSuchAlgorithmException, NoSuchPaddingException,
	                                                  InvalidKeyException, InvalidAlgorithmParameterException;
	public void decrypt(byte[] key, InputStream is, OutputStream os) throws IOException, 
    												NoSuchAlgorithmException, NoSuchPaddingException,
    												InvalidKeyException, InvalidAlgorithmParameterException;

	public byte[] sha256(InputStream is) throws IOException, NoSuchAlgorithmException;
		
}
