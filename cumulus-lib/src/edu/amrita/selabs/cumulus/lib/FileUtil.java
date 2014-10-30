package edu.amrita.selabs.cumulus.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.slf4j.Logger;

public class FileUtil {

	public static void close(InputStream is, Logger logger)
	{
		if(is == null) return;
		try
		{
			is.close();
		}catch(IOException e)
		{
			logger.error("Input Stream close failed", e);
		}
	}

	public static void close(OutputStream os, Logger logger)
	{
		if(os == null) return;
		try
		{
			os.close();
		}catch(IOException e)
		{
			logger.error("Output Stream close failed", e);
		}
	}

	public static void close(Reader r, Logger logger)
	{
		if(r == null) return;
		try
		{
			r.close();
		}catch(IOException e)
		{
			logger.error("Reader close failed", e);
		}
	}

	public static void close(Writer w, Logger logger)
	{
		if(w == null) return;
		try
		{
			w.close();
		}catch(IOException e)
		{
			logger.error("Writer close failed", e);
		}
	}

	public static void deleteFolder(File folder) 
	{
	    File[] files = folder.listFiles();
	    if(files!=null) 
	    { //some JVMs return null for empty dirs
	        for(File f: files) 
	        {
	        	if(f.isFile())
	        		f.delete();
	        	//else f.isFolder() deleteFolder(f)
	        }
	    }
	    folder.delete();
	}

	public static String getFileAsString(File f) throws IOException
	{
		assert(f != null);
		InputStreamReader rd = new InputStreamReader(new FileInputStream(f) );
		int i;
		StringBuilder buf = new StringBuilder();
		while((i=rd.read())!= -1)
		{
			buf.append((char)i);
		}
		rd.close();
		return buf.toString();
	}

	public static String getFileAsString(String s) throws IOException
	{
		return getFileAsString(new File(s));
	}

}
