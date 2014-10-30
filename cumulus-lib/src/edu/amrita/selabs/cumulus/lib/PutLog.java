package edu.amrita.selabs.cumulus.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace = "edu.amrita.selabs.cumulus.lib")
public class PutLog extends ActionLog{
	HashMap<Integer, PutLogEntry> entries;
	PutLogEntry fibEntry;
	
	public PutLog()
	{
		super();
		entries = new HashMap<Integer, PutLogEntry>();
		fibEntry = new PutLogEntry();
	}
		
	public PutLogEntry getFibEntry()
	{
		return fibEntry;
	}
	
	public void setFibEntry(PutLogEntry entry)
	{
		this.fibEntry = entry;
	}

			
	public HashMap<Integer, PutLogEntry> getEntries() {
		return entries;
	}

	public void setEntries(HashMap<Integer, PutLogEntry> entries) {
		this.entries = entries;
	}
	
	
	public PutLogEntry getEntry(int index)
	{
		return entries.get(new Integer(index));
	}
	

	public void putEntry(int index, String bid, String plainHash, HashSet<String> nodeids, int blockSize)
	{
		entries.put(new Integer(index), new PutLogEntry(bid, plainHash, nodeids, blockSize));
	}

	public void putEntry(int index, PutLogEntry entry)
	{
		entries.put(new Integer(index), entry);
	}
	
	public static PutLog loadIfExists(File workingFolder)
	{
		File putLogFile = new File(workingFolder.getAbsolutePath() + File.separator + ".cumulus.put.log");
		Reader rd = null;
		try
		{
			if(putLogFile.exists())
			{
				rd = new InputStreamReader(new FileInputStream(putLogFile));
				return fromXML(rd);
			}
		}catch(Exception e)
		{
			System.out.println(e + " PutLog file load failed. Deleting");
			if(putLogFile.exists())
				putLogFile.delete();
		}finally{
			try
			{
				if(rd != null)
					rd.close();
			}catch(IOException e)
			{
				//ignore
			}
		}
		return new PutLog();
	}
	
	public void store(File workingFolder) throws Exception
	{
		File putLogFile = new File(workingFolder.getAbsolutePath() + File.separator + ".cumulus.put.log");
		OutputStreamWriter wr = null;
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(putLogFile);
			wr = new OutputStreamWriter(out);
			toXML(wr);
			wr.flush();
			out.flush();
		}finally
		{
			if(out != null)
				out.close();
			if(wr != null)
				wr.close();
		}
	}
	
	public boolean allDone(int replicationFactor, int blockSize, long fileSize) throws Exception
	{
		return fibDone(replicationFactor) && allBlocksDone(replicationFactor, blockSize, fileSize) && nothingToNotifyServer();
	}
	
	public boolean fibDone(int replicationFactor)
	{
		if(this.fibEntry.getAlreadyPut().size() < replicationFactor)
			return false;
		else
			return true;
	}

	public boolean nothingToNotifyServer() throws Exception
	{

		for(int i =0;i< entries.size();i++)
		{
			Integer j = new Integer(i);
			PutLogEntry entry = entries.get(j);
			if(entry == null)
				throw new Exception("Invalid PutLog. Entry not found at index: " + i + ". Number of entries: " + entries.size());
			if(entry.getToNotifyServer().size() > 0)
				return false;
		}
		if(this.fibEntry.getToNotifyServer().size() > 0)
			return false;
		else
			return true;
	}
	
	public void doPendingNotifications(File workingFolder, String userName, String secret, String serverURL, HttpClient http) throws Exception
	{
		
		for(int i =0;i< entries.size();i++)
		{
			Integer j = new Integer(i);
			PutLogEntry entry = entries.get(j);
			if(entry == null)
				throw new Exception("Invalid PutLog. Entry not found at index: " + i + ". Number of entries: " + entries.size());
			HashSet<String> nodes = entry.getToNotifyServer();
			Iterator<String> it = nodes.iterator();			
			while(it.hasNext())
			{
				String n = it.next();
				http.setResponseType(HttpClient.ResponseType.XML);
				http.clearHeaders();				
				HttpClient.HttpResponse resp = http.post(serverURL,  "route", "putblockinfo", 
					  	"username", userName, 
					  	"secret", secret, 
					  	"type", "block",
					  	"blockid", entry.getBid(),
					  	"blocksize", Long.toString(entry.getBlockSize()),
					  	"nodeid", n
					  	);
				if(resp.getCode() == 200 && "success".equals(resp.getDocument().getDocumentElement().getAttribute("status")) )
				{
					it.remove();
					this.store(workingFolder);
				}
			}
		}

		PutLogEntry entry = this.getFibEntry();
		if(entry.getBid() != null)
		{
			HashSet<String> nodes = entry.getToNotifyServer();
			Iterator<String> it = nodes.iterator();
			while(it.hasNext())
			{
				String n = it.next();
				http.setResponseType(HttpClient.ResponseType.XML);
				HttpClient.HttpResponse resp = http.post(serverURL,  "route", "putblockinfo", 
					  	"username", userName, 
					  	"secret", secret, 
					  	"type", "iblock",
					  	"blockid", entry.getBid(),
					  	"nodeid", n
					  	);
				if(resp.getCode() == 200 && "success".equals(resp.getDocument().getDocumentElement().getAttribute("status")))
				{
					it.remove();
					this.store(workingFolder);
				}
			}
		}
		
	}

	public boolean allBlocksDone(int replicationFactor, int blockSize, long fileSize) throws Exception
	{
		int N = entries.size();
		long M = fileSize/blockSize;
		if(fileSize % blockSize > 0) M++;
		
		if(N < M)
			return false;
		for(int i =0;i< N;i++)
		{
			Integer j = new Integer(i);
			PutLogEntry entry = entries.get(j);
			if(entry == null)
				throw new Exception("Invalid PutLog. Entry not found at index: " + i + ". Number of entries: " + entries.size());
			if(entry.getAlreadyPut().size() < replicationFactor)
				return false;
		}
		return true;
	}
	
	public void loadFIB(FileInfoBlock fib, int blockSize) throws Exception
	{
		for(int i =0;i< entries.size();i++)
		{
			Integer j = new Integer(i);
			PutLogEntry entry = entries.get(j);
			if(entry == null)
				throw new Exception("Invalid PutLog. Entry not found at index: " + i + ". Number of entries: " + entries.size());
			fib.addDataBlock(entry.getBid(), entry.getPlainHash(), i * blockSize);
		}
	}
	
	public static PutLog fromXML(Reader rd) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(PutLog.class);		
		Unmarshaller um = context.createUnmarshaller();
		PutLog log = (PutLog) um.unmarshal(rd);
		return log;		
	}

	public String toXML() throws Exception
	{
		StringWriter wr = new StringWriter();
		toXML(wr);
		wr.close();
		return wr.toString();
	}

	public void toXML(Writer wr) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(PutLog.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
		m.marshal(this, wr);
	}


	@XmlRootElement(name = "PutLogEntry")
	@XmlType(propOrder = { "bid", "blockSize", "plainHash", "alreadyPut", "toNotifyServer"})		

	public static class PutLogEntry
	{
		String bid;
		String plainHash;
		long    blockSize;
		HashSet<String> alreadyPut;
		HashSet<String> toNotifyServer;
		
		public PutLogEntry()
		{
			bid =null;
			plainHash = null;
			blockSize = 0;
			alreadyPut = new HashSet<String>();
			toNotifyServer = new HashSet<String>();
		}
		
		public PutLogEntry(String bid, String plainHash, HashSet<String> nodeids, int blockSize)
		{
			this.bid = bid;
			this.alreadyPut =nodeids;
			this.plainHash = plainHash;
			toNotifyServer = new HashSet<String>();
			this.blockSize = blockSize;
		}
		
		public long getBlockSize()
		{
			return blockSize;
		}
		
		public void setBlockSize(long bs)
		{
			this.blockSize = bs;
		}

		public String getPlainHash() {
			return plainHash;
		}

		public void setPlainHash(String plainHash) {
			this.plainHash = plainHash;
		}
		
		public String getBid() {
			return bid;
		}
		public void setBid(String bid) {
			this.bid = bid;
		}
		public HashSet<String> getAlreadyPut() {
			return alreadyPut;
		}
		public void setAlreadyPut(HashSet<String> nodeids) {
			this.alreadyPut = nodeids;
		}

		public HashSet<String> getToNotifyServer() {
			return toNotifyServer;
		}

		public void setToNotifyServer(HashSet<String> toNotifyServer) {
			this.toNotifyServer = toNotifyServer;
		}

	}

}
