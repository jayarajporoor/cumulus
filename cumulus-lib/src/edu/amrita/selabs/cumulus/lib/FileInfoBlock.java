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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace = "edu.amrita.selabs.cumulus.lib")
public class FileInfoBlock {

	String fileName;
	ArrayList<FIBEntry> entries;
	
	public FileInfoBlock()
	{
		 entries = new ArrayList<FIBEntry>();
		 fileName = "";
	}
	
	public void addDataBlock(String bid, String key, int offset)
	{
		entries.add(new FIBEntry(bid, key, offset));
	}
	
	public ArrayList<FIBEntry>  getEntries()
	{
		return entries;
	}
	
	public void setFileName(String f)
	{
		fileName = f;
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	public void setEntries(ArrayList<FIBEntry> entries)
	{
		this.entries = entries;
	}
	
	public static FileInfoBlock fromXML(String xml) throws Exception
	{
		return fromXML(new StringReader(xml));
	}

	public static FileInfoBlock fromXML(Reader rd) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(FileInfoBlock.class);		
		Unmarshaller um = context.createUnmarshaller();
		FileInfoBlock fib = (FileInfoBlock) um.unmarshal(rd);
		return fib;
	}
	
	public String toXML() throws Exception
	{
		StringWriter wr = new StringWriter();
		toXML(wr);
		return wr.toString();
	}

	public void toXML(Writer wr) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(FileInfoBlock.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
		m.marshal(this, wr);
		wr.close();
	}
	
	@XmlRootElement(name = "FIBEntry")
	@XmlType(propOrder = { "bid", "key", "offset"})		

	public static class FIBEntry
	{
	
		String bid;
		String key;
		int offset;
		
		public FIBEntry(String bid, String key, int offset)
		{
			this.bid = bid;
			this.key =key;
			this.offset = offset;
		}
		
		public FIBEntry()
		{
			
		}
		
		public void setBid(String bid)
		{
			this.bid = bid;
		}
		
		public void setKey(String key)
		{
			this.key = key;
		}
		
		public void setOffset(int offset)
		{
			this.offset = offset;
		}
		
	
		public String getBid()
		{
			return this.bid;
		}
		
		public String getKey()
		{
			return key;
		}
		
		public int getOffset()
		{
			return offset;
		}
		
	}
}
