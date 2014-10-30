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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class StringUtil {
	
	public static final String getVersion()
	{
		return "0.01";
	}
	
	public static final String toHexString(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder();
	    for (byte b : bytes) {
	        sb.append(String.format("%02X", b));
	    }
	    return sb.toString();
	}
	
	public static byte[] toByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static String implode(String glue, ArrayList<String> list)
	{
		final int s = list.size();
		StringBuilder buf = new StringBuilder();
		for(int i=0;i<s;i++)
		{
			buf.append(list.get(i));
			if(i < s - 1)
				buf.append(glue);
		}
		return buf.toString();
	}

	public static String implode(String glue, Set<String> list)
	{
		StringBuilder buf = new StringBuilder();
		boolean first = true;
		for(String s: list)
		{
			if(first)
			{
				first = false;
			}else
			{
				buf.append(glue);
			}
			buf.append(s);
		}
		return buf.toString();
	}
	
}
