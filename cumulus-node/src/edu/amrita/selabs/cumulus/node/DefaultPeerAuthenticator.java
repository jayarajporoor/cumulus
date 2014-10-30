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

package edu.amrita.selabs.cumulus.node;

import java.security.Key;

import java.security.MessageDigest;
import java.util.HashMap;

import javax.crypto.Cipher;

import edu.amrita.selabs.cumulus.lib.RSACryptoUtil;
import edu.amrita.selabs.cumulus.lib.RSAKeyUtil;
import edu.amrita.selabs.cumulus.lib.StringUtil;

public class DefaultPeerAuthenticator implements PeerAuthenticator{
	HashMap<String, Key> keys = new HashMap<String, Key>();
	RSAKeyUtil keyUtil = new RSAKeyUtil();
	RSACryptoUtil cryptoUtil = new RSACryptoUtil(); 
	
	public void addNode(String nid, String pubKey) throws Exception
	{
		Key k = keyUtil.decodePublicKey(pubKey);
		if(keys.containsKey(nid))
			keys.remove(nid);
		keys.put(nid,  k);
	}
	
	public Status doAuth(String nid, String data, String sign) throws Exception
	{
		if(keys.containsKey(nid))
		{
			Key k = keys.get(nid);
			if(cryptoUtil.verify(data,  sign, k))
				return Status.SUCCESS;
			else
				return Status.FAILED;
		}
 
		return Status.NODE_NOT_FOUND;
	}
}
