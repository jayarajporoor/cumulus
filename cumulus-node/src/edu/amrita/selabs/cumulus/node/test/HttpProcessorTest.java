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



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.Key;
import java.util.Properties;

import javax.crypto.Cipher;

import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.HttpClient;
import edu.amrita.selabs.cumulus.lib.RSACryptoUtil;
import edu.amrita.selabs.cumulus.lib.RSAKeyUtil;
import edu.amrita.selabs.cumulus.lib.FileUtil;
import edu.amrita.selabs.cumulus.node.DefaultPeerAuthenticator;
import edu.amrita.selabs.cumulus.node.HttpProcessor;
import edu.amrita.selabs.cumulus.node.PeerAuthenticator;
import edu.amrita.selabs.cumulus.node.PeerAuthenticator.Status;

import org.mockito.InOrder;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

public class HttpProcessorTest {
	
	String nodeid = "a1fa7caad6a008ee";
	String content = "Aum Amriteswaryai Namah!\n";
	String privKeyFile = "/tmp/cumulus.priv";

	Properties props = new Properties();
	@Test
	public void testfullyMocked() throws Exception{
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = mock(PeerAuthenticator.class);
		when(auth.doAuth(any(String.class),any(String.class),any(String.class))).thenReturn(Status.SUCCESS);
		HttpClient http = mock(HttpClient.class);		
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);//TODO: provide mocked CumulusService
		String req = "POST /blocks/12345678?nodeid=" + nodeid + "&nonce=1234567890 HTTP/1.1\r\n" +
		             "Content-Length: " + content.length() + "\r\n" +
				     "X-Auth-Sign: 1234" + "\r\n" +
				     "Content-Type:application/binary\r\n" +
		             "\r\n" + content;
		ByteBuffer buf = proc.getBuf();
		buf.put(req.getBytes());
		buf.flip();
		proc.process();
		assertTrue(proc.getMethod().equals("POST"));
		assertTrue(proc.getURL().equals("/blocks/12345678"));
		assertTrue(proc.getHeader("Content-Length").equals(""+content.length()));
		assertTrue(proc.getHeader("Content-Type").equals("application/binary"));
		verify(sch).write(ByteBuffer.wrap("HTTP/1.1 200 Ok\r\nContent-Length:0\r\n\r\n".getBytes()));
		assertTrue(FileUtil.getFileAsString(proc.getDataFile()).equals(content));
	}

	@Test
	public void testfullyMockedWithHostName() throws Exception{
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = mock(PeerAuthenticator.class);
		when(auth.doAuth(any(String.class),any(String.class),any(String.class))).thenReturn(Status.SUCCESS);
		HttpClient http = mock(HttpClient.class);		
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);
		String req = "POST /blocks/12345678?nodeid=" + nodeid + "&nonce=1234567890 HTTP/1.1\r\n" +
		             "Content-Length: " + content.length() + "\r\n" +
				     "X-Auth-Sign: 1234" + "\r\n" +
				     "Content-Type:application/binary\r\n" +
				     "Host: localhost:9090\r\n" + 
		             "\r\n" + content;
		ByteBuffer buf = proc.getBuf();
		buf.put(req.getBytes());
		buf.flip();
		proc.process();
		assertTrue(proc.getMethod().equals("POST"));
		assertTrue(proc.getURL().equals("/blocks/12345678"));
		assertTrue(proc.getHeader("Content-Length").equals(""+content.length()));
		assertTrue(proc.getHeader("Content-Type").equals("application/binary"));
		assertTrue(proc.getHeader("Host").equals("localhost:9090"));
		verify(sch).write(ByteBuffer.wrap("HTTP/1.1 200 Ok\r\nContent-Length:0\r\n\r\n".getBytes()));
		assertTrue(FileUtil.getFileAsString(proc.getDataFile()).equals(content));
	}

	@Test
	public void testfullyMockedWithBadRequest() throws Exception{
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = mock(PeerAuthenticator.class);
		when(auth.doAuth(any(String.class),any(String.class),any(String.class))).thenReturn(Status.SUCCESS);
		HttpClient http = mock(HttpClient.class);		
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);
		String req = "POST /blocks/12345678?nodeid=" + nodeid + "&nonce=1234567890\r\n" +
		             "Content-Length: " + content.length() + "\r\n" +
				     "X-Auth-Sign: 1234" + "\r\n" +
				     "Content-Type:application/binary\r\n" +
				     "Host: localhost:9090\r\n" + 
		             "\r\n" + content;
		ByteBuffer buf = proc.getBuf();
		buf.put(req.getBytes());
		buf.flip();
		proc.process();
		verify(sch).write(ByteBuffer.wrap("HTTP/1.1 400 Bad Request\r\nContent-Length:0\r\n\r\n".getBytes()));

	}
	
	@Test
	public void testfullyMockedParts() throws Exception{
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = mock(PeerAuthenticator.class);
		when(auth.doAuth(any(String.class),any(String.class),any(String.class))).thenReturn(Status.SUCCESS);	
		HttpClient http = mock(HttpClient.class);		
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);
		String req1 = "POST /blocks/123456";
		String req2 = "78?nodeid=" +nodeid + "&nonce=1234567890 HTTP/1.1\r\nContent-Length: " + content.length() + "\r";
		String req3 = "\nContent-Type:application/b";
		String req4 = "inary\r\nX-Auth-Sign:1234\r\n\r\n" + content;

		ByteBuffer buf = proc.getBuf();
		buf.put(req1.getBytes());
		buf.flip();		
		proc.process();

		buf.clear();
		buf.put(req2.getBytes());
		buf.flip();		
		proc.process();

		buf.clear();
		buf.put(req3.getBytes());
		buf.flip();		
		proc.process();

		buf.clear();
		buf.put(req4.getBytes());
		buf.flip();		
		proc.process();
		
		assertTrue(proc.getMethod().equals("POST"));
		assertTrue(proc.getURL().equals("/blocks/12345678"));
		assertTrue(proc.getHeader("Content-Length").equals(""+content.length()));
		assertTrue(proc.getHeader("Content-Type").equals("application/binary"));
		verify(sch).write(ByteBuffer.wrap("HTTP/1.1 200 Ok\r\nContent-Length:0\r\n\r\n".getBytes()));
		assertTrue(FileUtil.getFileAsString(proc.getDataFile()).equals(content));		
	}
	

	@Test
	public void testPartlyMockecWithoutSign() throws Exception{
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = new DefaultPeerAuthenticator();
		HttpClient http = new HttpClient();		
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);
		String req = "POST /blocks/12345678?nodeid=" + nodeid + "&nonce=1234567890 HTTP/1.1\r\n" +
		             "Content-Length: " + content.length() + "\r\n" +
				     "Content-Type:application/binary\r\n" +
		             "\r\n" + content;
		ByteBuffer buf = proc.getBuf();
		buf.put(req.getBytes());
		buf.flip();
		proc.process();
		assertTrue(proc.getMethod().equals("POST"));
		assertTrue(proc.getURL().equals("/blocks/12345678"));
		assertTrue(proc.getHeader("Content-Length").equals(""+content.length()));
		assertTrue(proc.getHeader("Content-Type").equals("application/binary"));
		verify(sch).write(ByteBuffer.wrap("HTTP/1.1 403 Unauthorized\r\nContent-Length:0\r\n\r\n".getBytes()));
		//assertTrue(Util.getFileAsString(proc.getDataFile()).equals(content));
	}
	
	@Test
	public void testPartlyMockecWithBadSign() throws Exception{
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = new DefaultPeerAuthenticator();
		HttpClient http = new HttpClient();		
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);
		String req = "POST /blocks/12345678?nodeid=" + nodeid + "&nonce=1234567890 HTTP/1.1\r\n" +
		             "Content-Length: " + content.length() + "\r\n" +
				     "Content-Type:application/binary\r\n" +
		             "X-Auth-Sign: 1234567890\r\n" +
		             "\r\n" + content;
		ByteBuffer buf = proc.getBuf();
		buf.put(req.getBytes());
		buf.flip();
		proc.process();
		assertTrue(proc.getMethod().equals("POST"));
		assertTrue(proc.getURL().equals("/blocks/12345678"));
		assertTrue(proc.getHeader("Content-Length").equals(""+content.length()));
		assertTrue(proc.getHeader("Content-Type").equals("application/binary"));
		verify(sch).write(ByteBuffer.wrap("HTTP/1.1 403 Unauthorized\r\nContent-Length:0\r\n\r\n".getBytes()));
		//assertTrue(Util.getFileAsString(proc.getDataFile()).equals(content));
	}

	@Test
	public void testPartlyMockecWithSign() throws Exception{
		String bid = "12345678";
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = new DefaultPeerAuthenticator();
		HttpClient http = new HttpClient();	
		RSAKeyUtil keyUtil = new RSAKeyUtil();
		Key privKey = keyUtil.decodePrivateKey(FileUtil.getFileAsString(privKeyFile));
		
		String sign = new RSACryptoUtil().sign(bid,  privKey);

				
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);
		String req = "POST /blocks/"+bid+"?nodeid=" + nodeid + "&nonce=1234567890 HTTP/1.1\r\n" +
		             "Content-Length: " + content.length() + "\r\n" +
				     "Content-Type:application/binary\r\n" +
		             "X-Auth-Sign: " + sign + "\r\n" +
		             "\r\n" + content;
		ByteBuffer buf = proc.getBuf();
		buf.put(req.getBytes());
		buf.flip();
		proc.process();
		assertTrue(proc.getMethod().equals("POST"));
		assertTrue(proc.getURL().equals("/blocks/12345678"));
		assertTrue(proc.getHeader("Content-Length").equals(""+content.length()));
		assertTrue(proc.getHeader("Content-Type").equals("application/binary"));
		verify(sch).write(ByteBuffer.wrap("HTTP/1.1 200 Ok\r\nContent-Length:0\r\n\r\n".getBytes()));
		assertTrue(FileUtil.getFileAsString(proc.getDataFile()).equals(content));
	}

	@Test
	public void testGetPartlyMockecWithSign() throws Exception{
		String bid = "12345678";
		SocketChannel sch = mock(SocketChannel.class);
		PeerAuthenticator auth = new DefaultPeerAuthenticator();
		HttpClient http = new HttpClient();	
		RSAKeyUtil keyUtil = new RSAKeyUtil();
		Key privKey = keyUtil.decodePrivateKey(FileUtil.getFileAsString(privKeyFile));
		
		String sign = new RSACryptoUtil().sign(bid,  privKey);

				
		HttpProcessor proc = new HttpProcessor("/tmp", sch, auth, http, props, "localhost", null);
		String req = "GET /blocks/"+bid+"?nodeid=" + nodeid + "&nonce=1234567890 HTTP/1.1\r\n" +
		             "Content-Length: 0\r\n"  +
		             "X-Auth-Sign: " + sign + "\r\n\r\n"; 
		ByteBuffer buf = proc.getBuf();
		buf.put(req.getBytes());
		buf.flip();
		proc.process();
		assertTrue(proc.getMethod().equals("GET"));
		assertTrue(proc.getURL().equals("/blocks/12345678"));
		assertTrue(proc.getHeader("Content-Length").equals(""+0));
		verify(sch).write(ByteBuffer.wrap(("HTTP/1.1 200 Ok\r\nContent-Length:" + content.length() +"\r\n\r\n").getBytes()));
		buf = ByteBuffer.allocate(proc.getBuf().capacity());
		buf.put(content.getBytes());
		buf.flip();
		verify(sch).write(buf);//not working.		
	}
	
}

