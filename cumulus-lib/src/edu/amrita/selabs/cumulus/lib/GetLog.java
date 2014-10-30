package edu.amrita.selabs.cumulus.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace = "edu.amrita.selabs.cumulus.lib")
public class GetLog extends GetOrDeleteLog {
	HashMap<String, GetLogEntry> entries;
	
	public GetLog()
	{
		super();
		entries = new HashMap<String, GetLogEntry>();
	}
			
	public 	HashMap<String, GetLogEntry> getEntries()
	{
		return entries;
	}
	
	public void setEntries(HashMap<String, GetLogEntry> entries)
	{
		this.entries = entries;
	}
	
	boolean noNodesAvailable(String bid)
	{
		GetLogEntry e = entries.get(bid);
		if(e != null)
		{
			return e.isNoNodesAvailable();
		}
		
		return false;
	}
	
	boolean fibNoNodesAvailable()
	{
		return fibEntry.isNoNodesAvailable();
	}
	
	public boolean gotAllBlocks(FileInfoBlock fib)
	{
		if(fib == null) return false;
		
		for(FileInfoBlock.FIBEntry entry: fib.getEntries())
		{
			GetLogEntry logEntry = entries.get(entry.getBid());
			if(logEntry == null)
				return false;
			if(!logEntry.isGotBlock())
				return false;
		}
		return true;
	}
		
	public static GetLog loadIfExists(File workingFolder)
	{
		File getLogFile = new File(workingFolder.getAbsolutePath() + File.separator + ".cumulus.get.log");
		Reader rd = null;
		try
		{
			if(getLogFile.exists())
			{
				rd = new InputStreamReader(new FileInputStream(getLogFile));
				return fromXML(rd);
			}
		}catch(Exception e)
		{
			System.out.println(e + " GetLog file load failed. Deleting");
			if(getLogFile.exists())
				getLogFile.delete();
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
		return new GetLog();
	}
	
	public void store(File workingFolder) throws Exception
	{
		File getLogFile = new File(workingFolder.getAbsolutePath() + File.separator + ".cumulus.get.log");
		OutputStreamWriter wr = null;
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(getLogFile);
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
	
	public static GetLog fromXML(Reader rd) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(GetLog.class);		
		Unmarshaller um = context.createUnmarshaller();
		GetLog log = (GetLog) um.unmarshal(rd);
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
		JAXBContext context = JAXBContext.newInstance(GetLog.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
		m.marshal(this, wr);
	}

	@XmlRootElement(name = "GetLogEntry")
	@XmlType(propOrder = { "noNodesAvailable", "badNodes", "gotBlock"})		

	public static class GetLogEntry
	{
		boolean noNodesAvailable;
		
		HashSet<String> badNodes;
		boolean 		gotBlock;
		
		public GetLogEntry()
		{
			noNodesAvailable = false;
			badNodes = new HashSet<String>();
			gotBlock = false;
		}
		
		public boolean isGotBlock() {
			return gotBlock;
		}

		public void setGotBlock(boolean gotBlock) {
			this.gotBlock = gotBlock;
		}

		public boolean isNoNodesAvailable() {
			return noNodesAvailable;
		}

		public void setNoNodesAvailable(boolean noNodesAvailable) {
			this.noNodesAvailable = noNodesAvailable;
		}

		public HashSet<String> getBadNodes() {
			return badNodes;
		}

		public void setBadNodes(HashSet<String> badNodes) {
			this.badNodes = badNodes;
		}
		
		boolean nodeHasBadBlock(String nodeid)
		{
			return badNodes.contains(nodeid);
		}		
		
	}
	
}
