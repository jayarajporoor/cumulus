package edu.amrita.selabs.cumulus.lib.test;

import static org.junit.Assert.*;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.Key;

import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.HttpClient;
import edu.amrita.selabs.cumulus.lib.RSACryptoUtil;
import edu.amrita.selabs.cumulus.lib.RSAKeyUtil;
import edu.amrita.selabs.cumulus.lib.FileUtil;

public class HttpClientUploadTest {
	
	String privKeyFile = "/tmp/cumulus.priv";
	String nodeid = "a1fa7caad6a008ee";	

	//@Test
	public void testUpload() throws Exception{
		String bid = "1234567890";
		String url = "http://192.160.168.92:9090/blocks/" + bid;

		HttpClient http = new HttpClient();	
		RSAKeyUtil keyUtil = new RSAKeyUtil();
		Key privKey = keyUtil.decodePrivateKey(FileUtil.getFileAsString(privKeyFile));
		
		String sign = new RSACryptoUtil().sign(bid,  privKey);
		
		http.setHeader("X-Auth-Sign",  sign);
		
		String data = "Aum Amriteswaryai Namah!\n Lokah Samastah Sukhino Bhavantu";
		
		ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());
		HttpClient.HttpResponse resp = http.postStream(in,  url,  "nonce", "123456", "nodeid", nodeid);
		assertTrue(resp.getCode() == 200);
	}
	
	@Test
	public void testUpload2() throws Exception {
		HttpClient http = new HttpClient();
		FileInputStream in = new FileInputStream("/home/jayaraj/cumulus/jayaraj-ws/brahmamurari.mp3");
		HttpClient.HttpResponse r = http.postStream(in, "http://192.168.160.123:8080/test.php");
		System.out.println(r.getBody());
		/*in = new FileInputStream("/home/jayaraj/cumulus/jayaraj-ws/brahmamurari.mp3");
		r = http.postStream(in, "http://192.168.160.170/test.php");
		System.out.println(r.getBody());		
		in = new FileInputStream("/home/jayaraj/cumulus/jayaraj-ws/brahmamurari.mp3");
		r = http.postStream(in, "http://192.168.160.170/test.php");
		System.out.println(r.getBody());*/		 
	}

}
