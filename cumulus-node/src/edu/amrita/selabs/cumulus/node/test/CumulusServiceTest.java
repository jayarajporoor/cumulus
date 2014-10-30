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

package edu.amrita.selabs.cumulus.node.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import edu.amrita.selabs.cumulus.node.CumulusService;

public class CumulusServiceTest {

	@Test
	public void testCreateNode() throws Exception{
		Properties props = new Properties();
		CumulusService svc = new CumulusService(props);
		
		File f = File.createTempFile("CumulusServiceTest", Long.toString(System.nanoTime()));

	    if(!(f.delete()))
	    {
	        throw new Exception("Could not delete temp file: " + f.getAbsolutePath());
	    }	
	    
	    File tmpFolder = f.getParentFile();
		svc.createNode();
	}

}
