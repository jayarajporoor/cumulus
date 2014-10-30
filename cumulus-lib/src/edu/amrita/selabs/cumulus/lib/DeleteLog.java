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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace = "edu.amrita.selabs.cumulus.lib")
public class DeleteLog extends GetOrDeleteLog{
	HashMap<String, DeleteLogEntry>  entries; //for a particular block the set of nodes to delete the block
	DeleteLogEntry delFibEntry;
	
	public DeleteLog()
	{
		super();
		entries = new HashMap<String, DeleteLogEntry >();
		delFibEntry = null;//must be set null to denote that we have not yet loaded and then cleared the entry.
	}

	public HashMap<String, DeleteLogEntry> getEntries(){
		return entries;
	}

	public void setEntries(HashMap<String, DeleteLogEntry> entries) {
		this.entries = entries;
	}

	public DeleteLogEntry getDelFibEntry() {
		return delFibEntry;
	}

	public void setDelFibEntry(DeleteLogEntry entry) {
		this.delFibEntry = entry;
	}
	
	public boolean allBlocksDone(FileInfoBlock fib)
	{
		if(fib == null) return false;
		
		for(FileInfoBlock.FIBEntry entry: fib.getEntries())
		{
			DeleteLogEntry logEntry = entries.get(entry.getBid());
			if(logEntry == null)
			{
				return false;
			}else
			if(logEntry.getNodesToNotifyServer().size() > 0 || logEntry.getNodesToDelete().size() > 0)
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean fibDone()
	{
		if(this.delFibEntry == null)
			return false;
		if(this.delFibEntry.getNodesToDelete().size() > 0 || this.delFibEntry.getNodesToNotifyServer().size() > 0)
			return false;
		return true;
	}
	
	public static DeleteLog loadIfExists(File workingFolder)
	{
		File delLogFile = new File(workingFolder.getAbsolutePath() + File.separator + ".cumulus.delete.log");
		Reader rd = null;
		try
		{
			if(delLogFile.exists())
			{
				rd = new InputStreamReader(new FileInputStream(delLogFile));
				return fromXML(rd);
			}
		}catch(Exception e)
		{
			System.out.println(e + " DeleteLog file load failed. Deleting");
			if(delLogFile.exists())
				delLogFile.delete();
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
		return new DeleteLog();
	}
	
	public void store(File workingFolder) throws Exception
	{
		File delLogFile = new File(workingFolder.getAbsolutePath() + File.separator + ".cumulus.delete.log");
		OutputStreamWriter wr = null;
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(delLogFile);
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
	
	public static DeleteLog fromXML(Reader rd) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(DeleteLog.class);		
		Unmarshaller um = context.createUnmarshaller();
		DeleteLog log = (DeleteLog) um.unmarshal(rd);
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
		JAXBContext context = JAXBContext.newInstance(DeleteLog.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
		m.marshal(this, wr);
	}
	
	public static class DeleteLogEntry
	{
		HashSet<String>  nodesToDelete;
		HashSet<String>  deletedNodes;
		HashSet<String>  nodesToNotifyServer;
		
		public DeleteLogEntry()
		{
			nodesToDelete = new HashSet<String>();
			nodesToNotifyServer = new HashSet<String>();
			deletedNodes = new HashSet<String>();
		}
		
		public HashSet<String> getDeletedNodes() {
			return deletedNodes;
		}

		public void setDeletedNodes(HashSet<String> deletedNodes) {
			this.deletedNodes = deletedNodes;
		}
		

		public HashSet<String> getNodesToDelete() {
			return nodesToDelete;
		}

		public void setNodesToDelete(HashSet<String> nodesToDelete) {
			this.nodesToDelete = nodesToDelete;
		}

		public HashSet<String> getNodesToNotifyServer() {
			return nodesToNotifyServer;
		}

		public void setNodesToNotifyServer(HashSet<String> nodesToNotifyServer) {
			this.nodesToNotifyServer = nodesToNotifyServer;
		}
				
	}
	
}
