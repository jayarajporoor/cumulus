package edu.amrita.selabs.cumulus.node;

import java.io.File;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.amrita.selabs.cumulus.lib.FileUtil;

@XmlRootElement(namespace = "edu.amrita.selabs.cumulus.lib")
public class BlockMetaInfo {
	HashSet<String> fileids;
	static Logger logger = LoggerFactory.getLogger(BlockMetaInfo.class);
	
	public BlockMetaInfo()
	{
		fileids = new HashSet<String>();	
	}

	public HashSet<String> getFileids() {
		return fileids;
	}

	public void setFileids(HashSet<String> fileids) {
		this.fileids = fileids;
	}
	
	public static BlockMetaInfo loadIfExists(File folder, String blockid)
	{
		File metaFile = new File(folder.getAbsolutePath() + File.separator + blockid + ".meta");
		Reader rd = null;
		try
		{
			if(metaFile.exists())
			{
				rd = new InputStreamReader(new FileInputStream(metaFile));
				return fromXML(rd);
			}
		}catch(Exception e)
		{
			logger.error("BlockMetaInfo file load failed. Deleting: " + metaFile, e);
			if(metaFile.exists())
				metaFile.delete();
		}finally{
			FileUtil.close(rd, logger);
			rd = null;
		}
		return new BlockMetaInfo();
	}
	
	public void store(File folder, String blockid) throws Exception
	{
		File metaFile = new File(folder.getAbsolutePath() + File.separator + blockid + ".meta");
		OutputStreamWriter wr = null;
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(metaFile);
			wr = new OutputStreamWriter(out);
			toXML(wr);
			wr.flush();
			out.flush();
		}finally
		{
			FileUtil.close(out, logger);
			out = null;
			FileUtil.close(wr, logger);
			wr = null;
		}
	}

	public static BlockMetaInfo fromXML(Reader rd) throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(BlockMetaInfo.class);		
		Unmarshaller um = context.createUnmarshaller();
		BlockMetaInfo meta = (BlockMetaInfo) um.unmarshal(rd);
		return meta;		
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
		JAXBContext context = JAXBContext.newInstance(BlockMetaInfo.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
		m.marshal(this, wr);
	}
	
}
