package edu.amrita.selabs.cumulus.lib.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.CumulusClient;
import edu.amrita.selabs.cumulus.lib.DefaultCumulusClient;
import edu.amrita.selabs.cumulus.lib.DeleteInfo;
import edu.amrita.selabs.cumulus.lib.GetLog;
import edu.amrita.selabs.cumulus.lib.GetInfo;
import edu.amrita.selabs.cumulus.lib.PutLog;
import edu.amrita.selabs.cumulus.lib.FileUtil;
import edu.amrita.selabs.cumulus.lib.HttpClient.HttpResponse;

public class DefaultCumulusClientFullTest {
	Properties props = new Properties();
	String encPrivKey = "";
	@Before
	public void setup() throws Exception
	{
		props.setProperty("cumulus.server.url", "http://localhost/cumulus/cumulus-svr/router.php");
		props.setProperty("cumulus.nodename", "test");
		props.setProperty("cumulus.username", "test");
		props.setProperty("cumulus.sitename", "test");
		props.setProperty("cumulus.keyfolder", "/tmp");
		props.setProperty("cumulus.secret", "ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae");
		props.setProperty("cumulus.nodeid", "a1fa7caad6a008ee");
		encPrivKey = FileUtil.getFileAsString(props.getProperty("cumulus.keyfolder") + File.separator + "cumulus.priv");
	}

	//@Test
	public void testPut() throws Exception{
		this.getClass().getClassLoader().setDefaultAssertionStatus(true);
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);
		
		//File file = File.createTempFile("testPutBlock", ".dat");
		File file = new File("/tmp/cumulus_testPutBlock.dat");		
		OutputStream out = new FileOutputStream(file);
		
		final int blockSize = dcc.getBlockSize();
		final byte[] buf = new byte[blockSize];
		final int nBlocks = 10;		
		final int delta = 1000;
		
		
		for(int i =0;i< nBlocks; i++)
		{
			Arrays.fill(buf,  (byte) ('A' + i));
			if(i < nBlocks - 1)
				out.write(buf);
			else
				out.write(buf,  0,  buf.length - delta);
		}
		out.close();
		
		PutLog log = dcc.put(file.getAbsolutePath());
		file.delete();

	}

	//@Test
	public void testPut2() throws Exception{
		this.getClass().getClassLoader().setDefaultAssertionStatus(true);
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);
		
		//File file = File.createTempFile("testPutBlock", ".dat");
		File file = new File("/tmp/cumulus_testPutBlock2.dat");		
		OutputStream out = new FileOutputStream(file);
		
		final int blockSize = dcc.getBlockSize();
		final byte[] buf = new byte[blockSize];
		final int nBlocks = 10;		
		final int delta = 1000;
		
		
		for(int i =0;i< nBlocks; i++)
		{
			Arrays.fill(buf,  (byte) ('C' + i));
			if(i < nBlocks - 1)
				out.write(buf);
			else
				out.write(buf,  0,  buf.length - delta);
		}
		out.close();
		
		PutLog log = dcc.put(file.getAbsolutePath());
		file.delete();

	}
	
	//@Test
	public void testGet() throws Exception{
		this.getClass().getClassLoader().setDefaultAssertionStatus(true);
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);
		//File file = File.createTempFile("testPutBlock", ".dat");
		GetInfo pair = dcc.get("cumulus_testPutBlock.dat", "/tmp");
		if(pair.getFib() == null)
			System.out.println("Could not fetch FIB");
	}

	//@Test
	public void testGet2() throws Exception{
		this.getClass().getClassLoader().setDefaultAssertionStatus(true);
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);
		//File file = File.createTempFile("testPutBlock", ".dat");
		GetInfo pair = dcc.get("cumulus_testPutBlock2.dat", "/tmp");
		if(pair.getFib() == null)
			System.out.println("Could not fetch FIB");
	}
	
	@Test
	public void testDelete() throws Exception{
		this.getClass().getClassLoader().setDefaultAssertionStatus(true);
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);
		//File file = File.createTempFile("testPutBlock", ".dat");
		DeleteInfo pair = dcc.delete("cumulus_testPutBlock.dat");
		if(pair.getFib() == null)
			System.out.println("Could not fetch FIB");
	}	

}

