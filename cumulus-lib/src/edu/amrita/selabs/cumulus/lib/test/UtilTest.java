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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.StringUtil;

public class UtilTest {
	
	@Test
	public void toHexStringTest()
	{
		byte[] b = {0x3e, 0x54, 0x71, 0x11, (byte)0xFE, (byte)0xFF};
		String s = StringUtil.toHexString(b);
		assertTrue(s.equals("3E547111FEFF"));
	}

	@Test
	public void toByteArrayText()
	{
		String s = "3E547111FEFF";
		byte[] b0 = {0x3e, 0x54, 0x71, 0x11, (byte)0xFE, (byte)0xFF};
		byte[] b = StringUtil.toByteArray(s);
		assertTrue(Arrays.equals(b, b0));
	}
	
	@Test
	public void implodeTest()
	{
		ArrayList<String> l = new ArrayList<String>();
		l.add("abc");
		l.add("def");
		l.add("efg");
		assertTrue(StringUtil.implode(",", l).equals("abc,def,efg"));
	}
	
}
