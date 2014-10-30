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
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.HttpClient;
import static edu.amrita.selabs.cumulus.lib.HttpClient.HttpResponse;

import org.mockito.InOrder;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;


public class HttpClientTest {

	@Test
	public void testPost() throws Exception{
		HttpClient util = new HttpClient();
		HttpResponse res = util.post("http://localhost/cumulus/cumulus-svr/router.php?route=keepalive",
								"username", "test",
								"secret","ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae",
								"nodename", "test-server",
								"avail_space", "10000000");
		
		assertTrue(res.getBody().startsWith("<response"));
		assertTrue(res.getBody().endsWith("</response>\n"));		
		System.out.println(res);
		//Thread.sleep(1000);
	}

	@Test
	public void testPost2() throws Exception{
		HttpClient util = new HttpClient();
		util.setTimeout(10);
		HttpResponse res = util.post("http://localhost/cumulus/cumulus-svr/router.php",
								"route", "keepalive",
								"username", "test",
								"secret","ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae",
								"nodename", "test-server",
								"avail_space", "15000000");
		assertTrue(res.getBody().startsWith("<response"));	
		assertTrue(res.getBody().endsWith("</response>\n"));		
		System.out.println(res);
		//Thread.sleep(500);
	}

	
	@Test
	public void testGet() throws Exception{
		HttpClient util = new HttpClient();
		HttpResponse res = util.get("http://localhost/cumulus/cumulus-svr/router.php",
								"route", "keepalive",
								"username", "test",
								"secret","ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae",
								"nodename", "test-server");
		assertTrue(res.getBody().startsWith("<response"));	
		assertTrue(res.getBody().endsWith("</response>\n"));		
		System.out.println(res);
	}
	
	@Test
	public void testReadResponseAsXML() throws Exception
	{
		String r = "<response verion=\"0.01\" status=\"success\"><node><id>1234</id></node></response>";
		final ByteArrayInputStream in = new ByteArrayInputStream(r.getBytes());
		HttpClient http = new HttpClient();
		Method method = http.getClass().getDeclaredMethod("readResponseAsXML", HttpURLConnection.class);
		method.setAccessible(true);
		
		HttpURLConnection conn = mock(HttpURLConnection.class);
		when(conn.getInputStream()).thenReturn(in);
				
		HttpClient.HttpResponse resp =  (HttpClient.HttpResponse) method.invoke(http, conn);
		assertTrue(resp.getDocument().getDocumentElement().getNodeName().equals("response"));
		System.out.println(resp);

	}
	
}
