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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.DefaultBlockUploader;
import edu.amrita.selabs.cumulus.lib.NodeInfo;
import edu.amrita.selabs.cumulus.lib.PutLog;
import edu.amrita.selabs.cumulus.lib.StringUtil;

/*
public class DefaultBlockUploaderTest {
	Properties props = new Properties();
	String encPrivKey = "";
	@Before
	public void setup() throws Exception
	{
		props.setProperty("cumulus.server.url", "http://localhost/cumulus/cumulus-svr/router.php");
		props.setProperty("cumulus.nodename", "test");
		props.setProperty("cumulus.username", "test");
		props.setProperty("cumulus.secret", "ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae");
		props.setProperty("cumulus.sitename",  "test");
		props.setProperty("cumulus.keyfolder",  "/tmp");
		props.setProperty("cumulus.nodeid", "a1fa7caad6a008ee");
		encPrivKey = Util.getFileAsString(props.getProperty("cumulus.keyfolder") + File.separator + "cumulus.priv");
	}

	@Test
	public void testGetNodes() throws Exception{
		DefaultBlockUploader uploader = new DefaultBlockUploader();
		uploader.setProperties(props);
		ArrayList<String> dontuse = new ArrayList<String>();
		Method method = uploader.getClass().getDeclaredMethod("getNodes", String.class, long.class, ArrayList.class);
		method.setAccessible(true);
		
		NodeInfo[] nodes = (NodeInfo[]) 
							method.invoke(uploader, "3EFF43BF", 500L, dontuse);
		for(NodeInfo node: nodes)
		{
			System.out.println(node);
		}
	}
	
	@Test	
	public void testPutBlock() throws Exception{
		DefaultBlockUploader uploader = new DefaultBlockUploader();
		uploader.setProperties(props);
		String bid = "1234567890AB";
		String fileid = bid;
		String data = "Test Block\n Aum Amriteswaryai Namah!";
		ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());
		PutLog log = new PutLog();
		PutLog.PutLogEntry entry = new PutLog.PutLogEntry();
		uploader.putBlock(bid,  fileid, in,  data.length(), encPrivKey, entry, log, new File("/tmp"));
	}

}
*/