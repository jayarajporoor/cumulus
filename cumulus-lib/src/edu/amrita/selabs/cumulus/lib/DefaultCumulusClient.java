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

import java.io.BufferedInputStream;

import java.io.DataInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.ConnectException;
import java.nio.channels.SocketChannel;
import java.security.DigestInputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;

import org.bouncycastle.util.Arrays;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import edu.amrita.selabs.cumulus.lib.HttpClient.ResponseType;

public class DefaultCumulusClient implements CumulusClient{

	public static final int minBlockSize = 512 * 1024;
	
	Logger logger;
	
	Properties props;
	
	int blockSize = minBlockSize;
	
	ProgressCallback progress = new DummyProgress();
	
	String toplevelWorkingFolder;
	
	public DefaultCumulusClient()
	{
		logger = LoggerFactory.getLogger(this.getClass());
		toplevelWorkingFolder = createDefaultToplevelWorkingFolder();
	}
	
	public void setProgressCallback(ProgressCallback progress)
	{
		this.progress = progress;
	}
	
	public void setProperties(Properties props)
	{
		this.props = new Properties(props);
		String bs =  props.getProperty("cumulus.blocksize");
		if(bs != null)
		{
			try
			{
				int k = Integer.parseInt(bs);
				if(k % minBlockSize == 0)
					blockSize = k;
				else
					logger.error("Invalid Block Size: {}. Must be multiple of: {}. Defaulting to: {}", bs, minBlockSize, blockSize);
			}catch(Exception e)
			{
				logger.error("Invalid Block Size: " + bs + ". Defaulting to: " + minBlockSize, e);
			}
		}
		
		String wFolder = props.getProperty("cumulus.workingfolder");
		if(wFolder != null)
		{
			toplevelWorkingFolder = new File(wFolder).getAbsolutePath() + File.separator + ".cumulus";
			File folder = new File(toplevelWorkingFolder);
			if(!folder.exists())
				folder.mkdir();
		}
	}
	
	public void setBlockSize(int bs)
	{
		this.blockSize = bs;
	}
	
	public int getBlockSize()
	{
		return blockSize;
	}
		
	public GetInfo get(String filename, String targetFolderName) throws Exception
	{
		GetInfo gInfo = new GetInfo();
		
		File workingFolder = createWorkingFolder(toplevelWorkingFolder, filename, "get");
			
		GetLog log = null;
		
		try
		{
			log = GetLog.loadIfExists(workingFolder);
			
			gInfo.setLog(log);
			gInfo.setStatus(GetInfo.Status.IN_PROGRESS);
			HttpClient http = new HttpClient();
			
			DefaultBlockDownloader downloader = new DefaultBlockDownloader();

			downloader.setProperties(props);
			
			progress.taskBegin("getFIB");
			FileInfoBlock fib = null;
			boolean fileNotFound = false;
			try
			{
				fib = getFIB(workingFolder, filename, downloader, http, log);
			}catch(IOException e)
			{
				if(e.getMessage().equals("block_not_found"))
				{
					fileNotFound = true;
				}else
					throw e;
			}
			gInfo.setFib(fib);
			
			if(fib == null)
			{
				if(fileNotFound)
				{
					FileUtil.deleteFolder(workingFolder);					
					gInfo.setStatus(GetInfo.Status.FILE_NOT_FOUND);
					progress.taskError("File not found");
					logger.info("File not found in the system: {}", filename);
				}else
				{
					progress.taskError("Could not get FIB");
					logger.error("Could not fetch FileInfoBlock");
				}
				return gInfo;
			}
			
			progress.taskEnd();
			
			Set<String> remainingBlocks = new HashSet<String>();
			boolean gotAll = true;
			
			progress.taskBegin("getBlock");
			for(FileInfoBlock.FIBEntry entry: fib.getEntries())
			{
				try
				{
					String blockFile = getBlock(workingFolder, entry, downloader, http, log);
					if(blockFile == null)
					{
						remainingBlocks.add(entry.getBid());
						gotAll = false;
					}
				}catch(Exception e)
				{
					gotAll = false;
					logger.info("getBlock() failed. Continuing to next block...", e);
				}
				progress.taskProgress(entry.getBid());
			}
			
			if(gotAll)
			{
				progress.taskEnd();				
				if(combineBlocks(workingFolder.getAbsolutePath(), targetFolderName, filename, fib))
				{
					gInfo.setStatus(GetInfo.Status.DONE);
					FileUtil.deleteFolder(workingFolder);
				}else
				{
					gInfo.setStatus(GetInfo.Status.COMBINE_FAILED);
				}
			}else
			{
				progress.taskPartEnd();
			}
			
		}catch(Exception e)
		{
			logger.error("get failed", e);
		}
		
		return gInfo;
		
	}
	
	public boolean combineBlocks(String workingFolder, String targetFolderName, String filename, FileInfoBlock fib) throws Exception
	{
		byte buf[] = new byte[minBlockSize];
		File file = new File(targetFolderName + File.separator + filename);
		FileOutputStream os=null;
		FileInputStream  is=null;
		
		progress.taskBegin("combineBlocks");
		try
		{
			os = new FileOutputStream(file);
		
			for(FileInfoBlock.FIBEntry entry: fib.getEntries())
			{
				 is = new FileInputStream(workingFolder + File.separator + entry.getBid() + ".block");
				 int n;
				 while((n = is.read(buf)) > 0)
				 {
					 os.write(buf,  0,  n);
				 }
				 progress.taskProgress(entry.getBid());
			}
			is.close();
			os.close();
			progress.taskEnd();
			return true;
		}catch(Exception e)
		{
			progress.taskError("Combine failed");
			throw e;
		}
		finally
		{
			if(is != null)
				is.close();
			if(os != null)
				os.close();
		}

	}
	
	String getBlock(File workingFolder, FileInfoBlock.FIBEntry entry, DefaultBlockDownloader downloader, HttpClient http, GetLog log) throws Exception
	{
		byte[] buf = new byte[minBlockSize];		
		File blockFile = new File(workingFolder + File.separator + entry.getBid() + ".block");
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");		
		if(blockFile.exists())
		{
			FileInputStream in =null;
			try
			{
				in = new FileInputStream(blockFile);
				DigestInputStream din = new DigestInputStream(in, sha256);
				while(din.read(buf) > 0);
				din.close();
				String digest = StringUtil.toHexString(sha256.digest());
				if(entry.getKey().equals(digest))
				{
					return blockFile.getAbsolutePath();
				}else
				{
					blockFile.delete();
				}
			}catch(Exception e)
			{
				blockFile.delete();
				logger.error("Block file exists - but read/processing failed. Deleting and Continuing", e);				
			}finally
			{
				in.close();
			}
		}
		
		//we've reached here because block file does not exist or is not valid.
		
		String privKeyFile = props.getProperty("cumulus.keyfolder", "") + File.separator + "cumulus.priv";
		
		File encBlockFile=null;
		RandomAccessFile out=null;
		FileInputStream is = null;
		FileOutputStream os = null;

		try
		{

			encBlockFile = new File(blockFile.getAbsolutePath() + ".encrypt");
			
			out = new RandomAccessFile(encBlockFile, "rw");
			
			GetLog.GetLogEntry logEntry = log.getEntries().get(entry.getBid());
			if(logEntry == null)
			{
				logEntry = new GetLog.GetLogEntry();
				log.getEntries().put(entry.getBid(), logEntry);
			}else
			{
				logEntry.setGotBlock(false);
				log.store(workingFolder);
			}

			String nodeid;
			if((nodeid=downloader.getBlock(entry.getBid(), http, out, FileUtil.getFileAsString(privKeyFile), log, workingFolder, logEntry)) == null)
			{
				logger.info("Download Block failed for block: {}", entry.getBid());
				return null;
			}
			
			out.close();
			
			is = new FileInputStream(encBlockFile);
			
			FileCryptor cryptor = new DefaultFileCryptor();
					
			os = new FileOutputStream(blockFile);
			
			cryptor.decrypt(StringUtil.toByteArray(entry.getKey()), is, os);
			
			is.close();
			os.close();
			
			//verify the block
			is = new FileInputStream(blockFile);
			DigestInputStream din = new DigestInputStream(is, sha256);
			while(din.read(buf) > 0);
			din.close();
			String digest = StringUtil.toHexString(sha256.digest());
			if(entry.getKey().equals(digest))
			{
				logEntry.setGotBlock(true);
				log.store(workingFolder);
				return blockFile.getAbsolutePath();
			}else
			{
				logger.error("Block verification failed for block: {}. Encrypted digests does not match. Required: {}. Got: {}", 
										entry.getBid(), entry.getKey(), digest);
				blockFile.delete();
				logEntry.getBadNodes().add(nodeid);
				log.store(workingFolder);
				return null;
			}					
		}finally
		{
			if(is != null)
				is.close();
			if(os != null)
				os.close();
			if(out != null)
				out.close();
			if(encBlockFile != null && encBlockFile.exists())
				encBlockFile.delete();
		}		
	}
	
	FileInfoBlock getFIB(File workingFolder, String filename, DefaultBlockDownloader downloader, HttpClient http, GetOrDeleteLog log) throws Exception
	{
		File fibFile = new File(workingFolder + File.separator + filename + ".fib");
		if(fibFile.exists())
		{
			FileInputStream in = new FileInputStream(fibFile);
			Reader rd = new InputStreamReader(in);
			try
			{
				FileInfoBlock fib =  FileInfoBlock.fromXML(rd);
				fib.setFileName(fibFile.getAbsolutePath());
				return fib;
			}catch(Exception e)
			{
				fibFile.delete();
				logger.error("Loading existing FIB file failed", e);
			}finally
			{
				in.close();
				rd.close();
			}
		}
		
		//we've reached here because FIB file doesnot exist or is not valid.
		
		String userName = props.getProperty("cumulus.username", "");
		String secret = props.getProperty("cumulus.secret", "");
		String nodeName = props.getProperty("cumulus.nodename", "");
		String siteName = props.getProperty("cumulus.sitename", "");
		String privKeyFile = props.getProperty("cumulus.keyfolder", "") + File.separator + "cumulus.priv";
		
		File encFibFile=null;
		RandomAccessFile out=null;
		FileInputStream is = null;
		FileOutputStream os = null;

		try
		{
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			
			String qualifiedFileName = "/" + siteName + "/"  + secret + "@" + userName + "/" +  nodeName + "/" + filename;
			
			String bid = StringUtil.toHexString(sha256.digest(qualifiedFileName.getBytes()));

			encFibFile = new File(fibFile.getAbsolutePath() + ".encrypt");
			
			out = new RandomAccessFile(encFibFile, "rw");
			
			GetLog.GetLogEntry logEntry = log.getFibEntry();			

			String nodeid;
			if((nodeid=downloader.getBlock(bid, http, out, FileUtil.getFileAsString(privKeyFile), log, workingFolder, logEntry, true)) == null)
			{
				//TODO log
				return null;
			}
			
			out.close();
			
			is = new FileInputStream(encFibFile);
			
			FileCryptor cryptor = new DefaultFileCryptor();
					
			os = new FileOutputStream(fibFile);
			
			byte[] key = sha256.digest((userName + ":" + secret).getBytes());
			
			cryptor.decrypt(key, is, os);
			
			is.close();
			os.close();
			
			is = new FileInputStream(fibFile);
			InputStreamReader rd = new InputStreamReader(is);

			try
			{
				FileInfoBlock fib = FileInfoBlock.fromXML(rd);
			
				fib.setFileName(fibFile.getAbsolutePath());
				
				return fib;
			}catch(Exception e)
			{
				logger.error("Parsing FIB XML failed. Received from node: " + nodeid, e);
				logEntry.getBadNodes().add(nodeid);
				log.store(workingFolder);				
				throw e;
			}			

		}finally
		{
			if(is != null)
				is.close();
			if(os != null)
				os.close();
			if(out != null)
				out.close();
			if(encFibFile != null && encFibFile.exists())
				encFibFile.delete();
		}
		
	}
	
	public PutLog put(String filepath) throws Exception
	{
		String userName = props.getProperty("cumulus.username", "");
		String secret = props.getProperty("cumulus.secret", "");		
		String nodeName = props.getProperty("cumulus.nodename", "");
		String siteName = props.getProperty("cumulus.sitename", "");
		String serverURL = props.getProperty("cumulus.server.url", "");		

		File file = new File(filepath);
		
		if(!file.exists() && !file.isFile())
		{
			logger.error("File does not exist or is not a file: {}", file);
			return null;
		}
		
		long fileSize = file.length();

		File workingFolder = createWorkingFolder(toplevelWorkingFolder, file.getName(), "put");
		
		PutLog log = PutLog.loadIfExists(workingFolder);

		int replicationFactor = Integer.parseInt(props.getProperty("cumulus.replication.factor", "1"));
		
		try
		{

			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			String qualifiedFileName = "/" + siteName + "/"  + secret + "@" + userName + "/" +  nodeName + "/" + file.getName();
			
			String fileid = StringUtil.toHexString(sha256.digest(qualifiedFileName.getBytes()));
			
			DefaultFileCryptor encryptor = new DefaultFileCryptor();
			encryptor.createReadBuf(minBlockSize);
		
			DefaultBlockUploader uploader = new DefaultBlockUploader();
		
			uploader.setProperties(props);

			String privKeyFile = props.getProperty("cumulus.keyfolder", "") + File.separator + "cumulus.priv";			
			String encodedPrivateKey = FileUtil.getFileAsString(privKeyFile);
			
			FileInfoBlock fib;
			if(!log.allBlocksDone(replicationFactor, blockSize, fileSize))
			{
				progress.taskBegin("splitAndUpload");
				fib = splitAndUpload(filepath, fileid, workingFolder, encryptor, uploader, encodedPrivateKey, log);
				if(log.allBlocksDone(replicationFactor, blockSize, fileSize))
					progress.taskEnd();
				else
					progress.taskPartEnd();
			}
			else
			{
				fib = new FileInfoBlock();
				fib.setFileName(file.getName());
				log.loadFIB(fib, blockSize);
			}
			if(!log.fibDone(replicationFactor))
			{
				progress.taskBegin("uploadFIB");
				uploadFIB(fib, fileid, workingFolder, encryptor, uploader, encodedPrivateKey, log);
				if(log.fibDone(replicationFactor))
					progress.taskEnd();
				else
					progress.taskPartEnd();
			}
			
			if(!log.nothingToNotifyServer())
			{
				progress.taskBegin("PendingNotifications");
				HttpClient http = new HttpClient();
				log.doPendingNotifications(workingFolder, userName, secret, serverURL, http);
				if(log.nothingToNotifyServer())
					progress.taskEnd();
				else
					progress.taskPartEnd();
			}
		}catch(Exception e)
		{
			progress.taskError("Exception");
			logger.error("Put Operation Error", e);
		}
		finally
		{
			if(log.allDone(replicationFactor, blockSize, fileSize))
			{
				FileUtil.deleteFolder(workingFolder);
			}
		}

		return log;
	}	
	
	void uploadFIB(FileInfoBlock fib, String fileid, File workingFolder, FileCryptor encryptor, BlockUploader uploader, String encodedPrivateKey, PutLog log) throws Exception
	{
		File plain = null;
		File encrypted = null;
		FileInputStream fIn = null;
		Writer wr = null;
		FileOutputStream fOut = null;

		String userName = props.getProperty("cumulus.username", "");
		String secret 	= props.getProperty("cumulus.secret", "");

		int replicationFactor = Integer.parseInt(props.getProperty("cumulus.replication.factor", "1"));
		
		if(log.getFibEntry().getAlreadyPut().size() >= replicationFactor)
			return;

		try
		{			
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

			plain = getTempBlockFile(workingFolder, 0, ".fib");
			
			fOut = new FileOutputStream(plain);
			
			wr = new OutputStreamWriter(fOut);
			
			fib.toXML(wr);
			
			wr.close();
			fOut.close();
			
			encrypted = new File(plain.getAbsolutePath() + ".encrypt");
			
			fIn = new FileInputStream(plain);
			
			fOut = new FileOutputStream(encrypted);
			
			byte[] hSecret = sha256.digest((userName + ":" + secret).getBytes());

			encryptor.encrypt(hSecret, fIn, fOut);
		
			fIn.close();
			fOut.close();
			
			long encryptedBlockSize = encrypted.length();
					
			PutLog.PutLogEntry logEntry = log.getFibEntry();
			
			logEntry.setBid(fileid);
			
			logEntry.setBlockSize(encryptedBlockSize);
			
			log.store(workingFolder);
			
			uploader.putBlock(fileid, fileid, encrypted, encryptedBlockSize, encodedPrivateKey, logEntry, log, workingFolder, true);		
			
		}finally
		{
			if(wr != null)
				wr.close();
			if(fIn != null)
				fIn.close();
			if(fOut != null)
				fOut.close();
			if(plain != null && plain.exists())
				plain.delete();
			if(encrypted != null && encrypted.exists())
				encrypted.delete();			
		}
	}
	
	FileInfoBlock splitAndUpload(String filepath, String fileid, File workingFolder, FileCryptor encryptor, 
													BlockUploader uploader, String encodedPrivateKey, PutLog log) throws Exception
	{
			
		byte[] buf = new byte[minBlockSize];
		int blockNum = 0;
		boolean done = false;
		File blockFile = null;
		File encBlockFile = null;
		FileInputStream fIn = null;
		RandomAccessFile rIn = null;
		FileOutputStream fOut = null;
		final int cipherBlockSize = encryptor.getCipherBlockSize();

		FileInfoBlock fib = new FileInfoBlock();
		
		//FileInputStream fin = new FileInputStream(filepath);
		rIn = new RandomAccessFile(filepath, "r");
						
		int replicationFactor = Integer.parseInt(props.getProperty("cumulus.replication.factor", "1"));
		
		try
		{
			File file = new File(filepath);
			fib.setFileName(file.getName());
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			
			//DigestInputStream din = new DigestInputStream(fin, sha256);
			
			do
			{
				logger.debug("Processing block number: {}", blockNum);
				progress.taskProgress("");
				
				PutLog.PutLogEntry entry = log.getEntry(blockNum);

				if(entry != null && entry.getAlreadyPut().size() >= replicationFactor)
				{
					int n, toSkip = blockSize;
					while((n=rIn.skipBytes(toSkip)) > 0) 
					{
						toSkip -= n;//skip the block
						if(toSkip <= 0)
							break;
					}
					if(n <= 0) //nothing was skipped so we must have reached the end of file.
						done = true;
					fib.addDataBlock(entry.getBid(), entry.getPlainHash(), blockNum * blockSize);					
				}
				else
				{
					blockFile = getTempBlockFile(workingFolder, blockNum);
					encBlockFile = new File(blockFile.getAbsolutePath() + ".encrypt");
					int len = checkHash(encBlockFile, entry, sha256, buf);
					if(encBlockFile.exists() && len > 0 && entry != null)
					{
						int n, toSkip = blockSize;
						while((n=rIn.skipBytes(toSkip)) > 0) 
						{
							toSkip -= n;//skip the block
							if(toSkip <= 0)
								break;
						}
						if(n <= 0) //nothing was skipped so we must have reached the end of file.
							done = true;

						try
						{
							uploader.putBlock(entry.getBid(), fileid, encBlockFile, len, encodedPrivateKey, entry, log, workingFolder);
						}catch(Exception e)
						{
							logger.error("Put Block Failed. Continuing", e);
						}
						fib.addDataBlock(entry.getBid(), entry.getPlainHash(), blockNum * blockSize);					
					}else
					{
						fOut = new FileOutputStream(blockFile);
						int totalWritten = readWriteBlock(rIn, sha256, fOut, buf);
						fOut.close();				
						if(totalWritten > 0)
						{
							if(entry == null)
								entry = new PutLog.PutLogEntry();
							byte[] digest = sha256.digest();
							fIn = new FileInputStream(blockFile);
							fOut = new FileOutputStream(encBlockFile);
							encryptor.encrypt(digest, fIn, fOut);
							fIn.close();
							fOut.close();
							fIn = new FileInputStream(encBlockFile);
							String bid = StringUtil.toHexString(encryptor.sha256(fIn));
							fIn.close();
							//long encryptedBlockSize = (totalWritten / cipherBlockSize) * cipherBlockSize;
							long encryptedBlockSize = encBlockFile.length();

							entry.setPlainHash(StringUtil.toHexString(digest));
							entry.setBid(bid);
							entry.setBlockSize(encryptedBlockSize);
							
							try
							{
								uploader.putBlock(bid, fileid, encBlockFile, encryptedBlockSize, encodedPrivateKey, entry, log, workingFolder);
							}catch(Exception e)
							{
								logger.error("PutBlock failed. Continuing...", e);
							}							
							finally
							{
								log.putEntry(blockNum, entry);
								log.store(workingFolder);
							}
							fib.addDataBlock(bid, StringUtil.toHexString(digest), blockNum * blockSize);
							if(entry.getAlreadyPut().size() >= replicationFactor)
								encBlockFile.delete();
						}else
						{
							done = true;
						}
						if(blockFile != null)
							blockFile.delete();
					}
				}
				//encBlockFile.delete();
				blockNum++;
			}while(!done);
		}
		finally
		{
			if(rIn != null)
				rIn.close();
			if(fIn != null)
				fIn.close();
			if(fOut != null)
				fOut.close();
			if(blockFile != null && blockFile.exists())
				blockFile.delete();			
			/*if(encBlockFile != null && encBlockFile.exists())
				encBlockFile.delete();*/			
		}
		return fib;
	}
	
	//returns greater than zero if computed hash matches. The value returned in the size of the file
	//else returns -1.
	public int checkHash(File file, PutLog.PutLogEntry entry, MessageDigest sha256, byte[] buf) throws Exception
	{
		if(entry == null)
			return -1;
		DigestInputStream in = null;
		FileInputStream fin = null;
		try
		{
			fin = new FileInputStream(file);
			in = new DigestInputStream(fin, sha256);
			int len=0,n=0;
			while((n=in.read(buf)) > 0) len += n;
			String digest = StringUtil.toHexString(sha256.digest());
			if(digest.equals(entry.getBid()))
				return  len;
			else
				return -1;
		}finally
		{
			sha256.reset();
			if(in != null)
				in.close();
			if(fin != null)
				fin.close();
		}
		
	}
	
	int readWriteBlock(RandomAccessFile in, MessageDigest digest, OutputStream out, byte[] buf) throws IOException
	{
		boolean done = false;
		int totalWritten = 0;
		do
		{
			int len = in.read(buf);
			if(len > 0)
			{
				digest.update(buf, 0, len);
				out.write(buf, 0, len);
				totalWritten += len;
			}else
				done = true;//EOF
			if(totalWritten >= blockSize)
				done = true;			
		}while(!done);
		
		return totalWritten;
	}
	
	File createWorkingFolder(String baseFolder, String filename, String reason) throws Exception
	{
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		String base = new File(baseFolder).getAbsolutePath();
		File f = new File(base + File.separator + "." + filename + "." + reason + "." + 
				StringUtil.toHexString(sha256.digest(filename.getBytes())));
		if(f.exists())
		{
			if(!f.isDirectory())
				throw new Exception("Cannot create working folder. File with that name exists and is not a folder: " + f);
		}else
		{
			if(!f.mkdir())
				throw new Exception("Cannot create working folder: " + f);
		}
		return f;
	}
	
	File getTempBlockFile(File workingFolder, int blockNum) throws IOException
	{
		return getTempBlockFile(workingFolder, blockNum, ".plain");
	}

	
	File getTempBlockFile(File workingFolder, int blockNum, String suffix) throws IOException
	{
		String blockFilePath = String.format("%s%s%05d%s", workingFolder.getAbsolutePath(), 
														   File.separator, blockNum, suffix);

		File blockFile = new File(blockFilePath);
		return blockFile;
	}
		
	public DeleteInfo delete(String filename) throws Exception
	{
		String userName = props.getProperty("cumulus.username", "");	
		String nodeName = props.getProperty("cumulus.nodename", "");
		String siteName = props.getProperty("cumulus.sitename", "");
		String secret   = props.getProperty("cumulus.secret", "");

		File innerWorkingFolder = createWorkingFolder(toplevelWorkingFolder, filename, "delete");
		
		DeleteInfo dInfo = new DeleteInfo();
		
		DeleteLog log = DeleteLog.loadIfExists(innerWorkingFolder);
		
		dInfo.setLog(log);
		
		FileInfoBlock fib = null;

		try
		{
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			String qualifiedFileName = "/" + siteName + "/"  + secret + "@" + userName + "/" +  nodeName + "/" + filename;
			
			String fileid = StringUtil.toHexString(sha256.digest(qualifiedFileName.getBytes()));

			HttpClient http = new HttpClient();
			
			DefaultBlockDownloader downloader = new DefaultBlockDownloader();
			DefaultBlockDeleter deleter = new DefaultBlockDeleter();

			downloader.setProperties(props);
			deleter.setProperties(props);
			
			progress.taskBegin("getFIB");
			
			boolean fileNotFound = false;
						
			try
			{
			
				fib = getFIB(innerWorkingFolder, filename, downloader, http, log);
			}catch(IOException e)
			{
				if(e.getMessage().equals("block_not_found"))
					fileNotFound = true;
				else
					throw e;
			}
						
			dInfo.setFib(fib);
			
			if(fib == null)
			{
				if(fileNotFound)
				{
					FileUtil.deleteFolder(innerWorkingFolder);					
					dInfo.setStatus(DeleteInfo.Status.FILE_NOT_FOUND);
					progress.taskError("File not found");
				}else
				{
					progress.taskError("FIB fetch failed");
				}
				return dInfo;
			}
			progress.taskEnd();
			
			progress.taskBegin("deleteBlock");
			
			for(FileInfoBlock.FIBEntry entry: fib.getEntries())
			{
				try
				{
					DeleteLog.DeleteLogEntry logEntry = log.getEntries().get(entry.getBid());
					boolean isFresh = false;
					if(logEntry == null)
					{
						isFresh = true;
						logEntry = new DeleteLog.DeleteLogEntry();
					}
					
					if(isFresh || logEntry.getNodesToDelete().size() > 0 || logEntry.getNodesToNotifyServer().size() > 0)
					{
						deleteBlock(innerWorkingFolder, fileid, entry, isFresh, false, log, logEntry, http, deleter);
					}
					progress.taskProgress(entry.getBid());
				}catch(Exception e)
				{
					logger.error("deleteBlock Failed. Continuing to next block", e);

				}
			}
			
			if(log.allBlocksDone(fib))
			{
				progress.taskEnd();
				DeleteLog.DeleteLogEntry logEntry = log.getDelFibEntry();
				boolean isFresh = false;
				if(logEntry == null)
				{
					isFresh = true;
					logEntry = new DeleteLog.DeleteLogEntry();
				}
				
				progress.taskBegin("FIB deleteBlock");
				if(isFresh || logEntry.getNodesToDelete().size() > 0 || logEntry.getNodesToNotifyServer().size() > 0)
				{
					deleteBlock(innerWorkingFolder, fileid, null, isFresh, true, log, logEntry, http, deleter);
				}
				
				if(log.fibDone())
				{
					progress.taskEnd();
				}else
					progress.taskPartEnd();
				
			}else
				progress.taskPartEnd();
			
		}catch(Exception e)
		{
			logger.error("Delete failed", e);
		}finally
		{
			if(log.allBlocksDone(fib) && log.fibDone())
			{
				dInfo.setStatus(DeleteInfo.Status.DONE);
				FileUtil.deleteFolder(innerWorkingFolder);
			}
		}
		return dInfo;
	}
	
	void deleteBlock(File workingFolder, String fileid, FileInfoBlock.FIBEntry fibEntry, boolean isFresh, boolean isInfoBlock, 
			DeleteLog log, 	DeleteLog.DeleteLogEntry logEntry, HttpClient http, DefaultBlockDeleter deleter) throws Exception
	{
		String userName = props.getProperty("cumulus.username", "");
		String secret = props.getProperty("cumulus.secret", "");				
		String serverURL = props.getProperty("cumulus.server.url", "");		
		String encodedPrivateKey = FileUtil.getFileAsString(props.getProperty("cumulus.keyfolder") + File.separator + "cumulus.priv");
		
		String blockid;
		String type;
		
		if(isInfoBlock)
		{
			blockid = fileid;
			type = "iblock";
		}
		else
		{
			blockid = fibEntry.getBid();
			type = "block";
		}

		if(isFresh)
		{
			http.setResponseType(HttpClient.ResponseType.XML);
			http.clearHeaders();						
			HttpClient.HttpResponse resp = http.post(serverURL,
													"route", "getblockinfo",
													"username", userName, 
													"secret", secret,
													"blockid", blockid,
													"type", type);

			Document doc  = resp.getDocument();
			if("success".equals(doc.getDocumentElement().getAttribute("status")))
			{
				NodeInfo[] nis =  NodeInfo.getNodeInfoArray(doc.getDocumentElement());
				for(NodeInfo n:nis)
				{
					logEntry.getNodesToDelete().add(n.getId());
				}
			}else
			{
				logger.error("getblockinfo returned error:{}", resp.getDocument().getDocumentElement().getTextContent());
			}
			if(isInfoBlock)
			{
				log.setDelFibEntry(logEntry);
			}
			else
			{
				log.getEntries().put(blockid,  logEntry);
			}
			log.store(workingFolder);
		}
		
		Iterator<String> it = logEntry.getNodesToDelete().iterator();

		while(it.hasNext())
		{
			String nodeid = it.next();
			DefaultBlockDeleter.DeleteStatus st = 
					deleter.deleteBlock(workingFolder, http, nodeid, blockid, fileid, log, encodedPrivateKey, isInfoBlock);
			if(st == DefaultBlockDeleter.DeleteStatus.FULL_DELETE || st == DefaultBlockDeleter.DeleteStatus.PARTIAL_DELETE)
			{
				it.remove();
				logEntry.getDeletedNodes().add(nodeid);
				if(st == DefaultBlockDeleter.DeleteStatus.FULL_DELETE)
				{
					logEntry.getNodesToNotifyServer().add(nodeid);
				}
				log.store(workingFolder);
			}else
			{
				logger.error("Error in deleting blockid {} from nodeid {}", blockid, nodeid);
			}
		}
		
		it = logEntry.getNodesToNotifyServer().iterator();
				
		while(it.hasNext())
		{
			String nodeid = it.next();
			http.setResponseType(HttpClient.ResponseType.XML);
			
			HttpClient.HttpResponse resp = http.post(serverURL,
													"route", "delblocknode",
													"username", userName, 
													"secret", secret,
													"blockid", blockid,
													"type", type,
													"nodeid", nodeid);

			Document doc  = resp.getDocument();
			if("success".equals(doc.getDocumentElement().getAttribute("status")))
			{
				it.remove();
				log.store(workingFolder);
			}
		}		
	}
	
	public String createDefaultToplevelWorkingFolder()
	{
		try
		{
			File file = File.createTempFile("cumulus",  "");
			file.delete();
			File wFolder = new File(file.getAbsoluteFile().getParentFile().getAbsolutePath() + File.separator + ".cumulus");
			wFolder.mkdir();
			return wFolder.getAbsolutePath();
		}catch(IOException e)
		{
			logger.error("Cannot create temporary file to obtain default working folder. Defaulting to present working directory.", e);
		}
		
		return ".";
	}	
}
