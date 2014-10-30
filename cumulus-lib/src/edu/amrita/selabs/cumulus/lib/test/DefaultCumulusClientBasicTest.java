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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import edu.amrita.selabs.cumulus.lib.BlockUploader;
import edu.amrita.selabs.cumulus.lib.CumulusClient;
import edu.amrita.selabs.cumulus.lib.DefaultBlockUploader;
import edu.amrita.selabs.cumulus.lib.DefaultCumulusClient;
import edu.amrita.selabs.cumulus.lib.DefaultFileCryptor;
import edu.amrita.selabs.cumulus.lib.FileCryptor;
import edu.amrita.selabs.cumulus.lib.FileInfoBlock;
import edu.amrita.selabs.cumulus.lib.PutLog;
import edu.amrita.selabs.cumulus.lib.StringUtil;

/*
public class DefaultCumulusClientBasicTest {
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
	public void testReadWriteBlock() throws Exception
	{
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		Method method = dcc.getClass().getDeclaredMethod("readWriteBlock", RandomAccessFile.class, MessageDigest.class, OutputStream.class, byte[].class);
		method.setAccessible(true);
		byte[] buf = new byte[dcc.minBlockSize];
		
		dcc.setBlockSize(dcc.minBlockSize * 2);
		
		byte[] bufIn = new byte[dcc.getBlockSize()];
		Arrays.fill(bufIn,  (byte)0x3E);
		ByteArrayInputStream bin = new ByteArrayInputStream(bufIn);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		method.invoke(dcc, bin, bout, buf);
		byte[] bufOut = bout.toByteArray();
		assertTrue(Arrays.equals(bufIn,  bufOut));
	}

	@Test
	public void testReadWriteSmallBlock() throws Exception
	{
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		Method method = dcc.getClass().getDeclaredMethod("readWriteBlock", InputStream.class, OutputStream.class, byte[].class);
		method.setAccessible(true);
		byte[] buf = new byte[dcc.minBlockSize];
		
		dcc.setBlockSize(dcc.minBlockSize * 2);
		
		byte[] bufIn = new byte[dcc.getBlockSize() - 1000];
		Arrays.fill(bufIn,  (byte)0x3E);
		ByteArrayInputStream bin = new ByteArrayInputStream(bufIn);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		method.invoke(dcc, bin, bout, buf);
		byte[] bufOut = bout.toByteArray();
		assertTrue(Arrays.equals(bufIn,  bufOut));
	}
	
	@Test
	public void testCreateTempBlockFile() throws Exception
	{
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);
		
		Method method = dcc.getClass().getDeclaredMethod("createTempBlockFile", String.class, int.class);
		method.setAccessible(true);
		String fileName = "test.tar.gz";
		int blockNum = 1;
		File file = (File) method.invoke(dcc, fileName, blockNum);
		
		assertTrue(file.getName().startsWith(fileName));
		String path = file.getAbsolutePath();
		System.out.println("Temp block file:" + path);
		file.delete();
	}

	@Test
	public void testSplitAndUpload() throws Exception{
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);		
		Method method = dcc.getClass().getDeclaredMethod("splitAndUpload",String.class, String.class,
															FileCryptor.class, BlockUploader.class, String.class);
		method.setAccessible(true);
		File file = File.createTempFile("testSplitAndUpload", ".dat");
		OutputStream out = new FileOutputStream(file);
		
		final int blockSize = dcc.getBlockSize();
		final byte[] buf = new byte[blockSize];
		final int nBlocks = 10;		
		final int delta = 1000;
		
		
		for(int i =0;i< nBlocks; i++)
		{
			Arrays.fill(buf,  (byte) ('0' + i));
			if(i < nBlocks - 1)
				out.write(buf);
			else
				out.write(buf,  0,  buf.length - delta);
		}
		out.close();

		FileCryptor encryptor = new FileCryptor() 
		{
			int nBlock = 0;
			
			public void createReadBuf(int readBlockSize)
			{
				
			}
			
			public int getCipherBlockSize()
			{
				return 16;
			}
			
			public void encrypt(byte[] hash, InputStream is, OutputStream os) throws IOException
			{
				int blockLen = 0;
				
				int nRead = is.read(buf);
				if( nBlock < nBlocks - 1)
				{
					blockLen = buf.length;
				}else
					blockLen = buf.length - delta;
				
				assertTrue(nRead == blockLen);
				assertTrue(is.read() == -1);
				for(int i=0;i<blockLen;i++)
				{
					assertTrue(buf[i] == (byte) ('0' + nBlock));
				}

				nBlock++;				
			}
			
			public void decrypt(byte[] hash, InputStream is, OutputStream os) throws IOException
			{
				
			}
				
			public byte[] sha256(InputStream is) throws IOException
			{
				byte[] bid = new byte[32];
				bid[bid.length-1] = (byte)nBlock;
				return bid;
			}
			
		};
		
		BlockUploader uploader = mock(BlockUploader.class);
		
		String fileid = "1234567890";
		
		FileInfoBlock fib = (FileInfoBlock) method.invoke(dcc,  file.getAbsolutePath(), fileid, encryptor, uploader, encPrivKey);
		
		file.delete();

		int nBlock = 0;
		InOrder inOrder = inOrder(uploader);
		for(FileInfoBlock.FIBEntry e: fib.getEntries())
		{
			System.out.println("Block id: " + e.getBid() + ", key: " + e.getKey() + ", offset: " + e.getOffset());
			inOrder.verify(uploader).putBlock(eq(e.getBid()), eq(fileid), any(InputStream.class), any(int.class), any(String.class), any(PutLog.PutLogEntry.class), any(PutLog.class), any(File.class));
			nBlock++;
		}
		assertTrue(nBlock == nBlocks);
		String xml = fib.toXML();
		System.out.println(xml);
		FileInfoBlock fib2 = FileInfoBlock.fromXML(xml);
		
		for(FileInfoBlock.FIBEntry e: fib.getEntries())
		{
			System.out.println("Block id: " + e.getBid() + ", key: " + e.getKey() + ", offset: " + e.getOffset());
		}
	
	}

	@Test
	public void testActualSplitAndUpload() throws Exception{
		this.getClass().getClassLoader().setDefaultAssertionStatus(true);
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProperties(props);		
		Method method = dcc.getClass().getDeclaredMethod("splitAndUpload",String.class, String.class, 
															FileCryptor.class, BlockUploader.class, String.class);
		method.setAccessible(true);
		File file = new File("/tmp/testSplitAndUpload.dat");
		OutputStream out = new FileOutputStream(file);

		String qualifiedFileName = "/test/test/test/testSplitAndUpload.dat";
		
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		
		String fileid = Util.toHexString(sha256.digest(qualifiedFileName.getBytes()));

		final int blockSize = dcc.getBlockSize();
		final byte[] buf = new byte[blockSize];
		final int nBlocks = 10;		
		final int delta = 1000;
		
		
		for(int i =0;i< nBlocks; i++)
		{
			Arrays.fill(buf,  (byte) ('0' + i));
			if(i < nBlocks - 1)
				out.write(buf);
			else
				out.write(buf,  0,  buf.length - delta);
		}
		out.close();

		FileCryptor encryptor = new DefaultFileCryptor();
		
		encryptor.createReadBuf(DefaultCumulusClient.minBlockSize);
		
		DefaultBlockUploader uploader = new DefaultBlockUploader();
		uploader.setProperties(props);		
		
		FileInfoBlock fib = (FileInfoBlock) method.invoke(dcc,  file.getAbsolutePath(), fileid, encryptor, uploader, encPrivKey);
		
		file.delete();

		int nBlock = 0;
		for(FileInfoBlock.FIBEntry e: fib.getEntries())
		{
			System.out.println("Block id: " + e.getBid() + ", key: " + e.getKey() + ", offset: " + e.getOffset());
			nBlock++;
		}
		assertTrue(nBlock == nBlocks);
	}
	
}
*/