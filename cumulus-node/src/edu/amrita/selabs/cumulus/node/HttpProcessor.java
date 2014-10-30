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

package edu.amrita.selabs.cumulus.node;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.amrita.selabs.cumulus.lib.HttpClient;
import edu.amrita.selabs.cumulus.lib.NodeInfo;
import edu.amrita.selabs.cumulus.lib.FileUtil;
import edu.amrita.selabs.cumulus.node.PeerAuthenticator.Status;


public class HttpProcessor {
	
	ByteBuffer buf = ByteBuffer.allocate(1024);
	HttpClient http;
	Properties props;
	Logger logger;
	String remoteAddr;
	CumulusService svc;
	
	public HttpProcessor(String dataPath, SocketChannel sch, PeerAuthenticator auth, HttpClient http, 
					Properties props, String remoteAddr, CumulusService svc)
	{
			this.dataPath = dataPath;
			this.socketChannel = sch;
			this.auth = auth;
			this.http = http;
			this.outputDataChannel= null;
			this.props = props;
			this.logger = LoggerFactory.getLogger(this.getClass());
			this.remoteAddr = remoteAddr;
			this.svc = svc;
	}
	
	public ByteBuffer getBuf()
	{
		return buf;
	}
	
	public void process() throws Exception
	{
		if(state == State.BODY)
		{
			if(outputDataChannel == null)
			{
				remainingContentLength -= buf.remaining();			
				/*while(buf.hasRemaining())
					buf.get();*/
			}else
			{
				remainingContentLength -= buf.remaining();			
				outputDataChannel.write(buf);
			}
			if(remainingContentLength <= 0)
			{
				if(outputDataChannel != null)
					outputDataChannel.close();
				done();				
				writeResponseHeaders("200 Ok", 0);
			}			
			return;
		}else
		if((state == State.DONE) || (state == State.OUTPUT))
		{
			while(buf.hasRemaining()) buf.get();//discard
			//TODO: log or throw error?
			return;
		}
		
		while(buf.hasRemaining())
		{	
			if(state == State.REQUEST_LINE || state == State.HEADER_FIELDS)
			{				
				char c = (char) buf.get();
				
				if(c == '\n')
				{
					crlfs++;
					if(state == State.REQUEST_LINE)
					{
						String s = sbuf.toString();
						if(logger.isTraceEnabled())
						{
							logger.trace("Request line from {}: {}", remoteAddr, s);
						}
						parseRequestLine(s);
						sbuf.setLength(0);
						if (state != State.DONE)
							state = State.HEADER_FIELDS;
					}else
					if(crlfs < 2)
					{
						String s = sbuf.toString();
						if(logger.isTraceEnabled())
						{
							logger.trace("Header Line from {}: {}", remoteAddr, s);
						}						
						parseHeaderField(s);
						sbuf.setLength(0);
					}
				
					if(crlfs >= 2)
					{
						Status s = checkAuth();
						if(s == Status.NODE_NOT_FOUND)
						{
							s = fetchAndCheckAuth();
						}

						if(s == Status.SUCCESS)
						{
							state = State.BODY;							
							startProcessingBody();
						}
						else
						{
							logger.info("Authentication Failure. Remote Addr: {}", remoteAddr);
							writeResponseHeaders("403 Unauthorized", 0);
							done();
							while(buf.hasRemaining()) buf.get();//discard remaining
						}
					}
				}
				prevChar = c;
				if(c != '\r' && c != '\n')
				{
					sbuf.append(c);
					crlfs = 0;
				}
			
			}else
			{
				while(buf.hasRemaining())//discard remaining bytes
					buf.get();
			}
		}
				
	}
	
	public State getState()
	{
		return state;
	}
	
	void done()
	{
		state = State.DONE;//need to set this before close.
		close();
		if("POST".equals(method))
		{
			String fileid = headerFields.get("X-File-ID");
			if(fileid != null && !isInfoBlock)
			{
				File path = new File(dataPath);
				BlockMetaInfo meta = BlockMetaInfo.loadIfExists(path, blockId);
				
				meta.getFileids().add(fileid);
				
				try
				{
					meta.store(path, blockId);
				}catch(Exception e)
				{
					logger.error("meta.store() failed. Block Id: " + blockId, e);
				}
				
			}
		}

	}

	public void close() 
	{
		if(outputDataChannel != null)
		{
			try
			{
				outputDataChannel.close();
			}catch(IOException e)
			{
				logger.error("Could not close output data channel", e);
			}
			outputDataChannel = null;
		}
		if(outputDataStream != null)
			FileUtil.close(outputDataStream, logger);
		if(inputDataStream != null)
			FileUtil.close(inputDataStream, logger);
		outputDataStream = null;
		inputDataStream = null;
		//if we were in the middle of a POST and the close is abrupt then delete the partial data file.
		if(method.equals("POST") && state != State.DONE)
		{
			File blockMeta = new File(dataPath + File.separator + blockId + ".meta");
			if(!blockMeta.exists()) //there is no existing file refering to this block.
			{
				File blockFile = new File(dataPath + File.separator + blockId + ".block");
				if(blockFile.exists())
				{
					if(logger.isDebugEnabled())
						logger.debug("Deleting partially received block file: {}", blockFile.getAbsolutePath());
					if(blockFile.delete())
						svc.updateRemainingSpace(-contentLength);
					else
						logger.error("Could not delete partially received block file: {}", blockFile.getAbsolutePath());
				}
			}
		}
	}
		
	void startProcessingBody() throws Exception
	{
		if(method.equals("DELETE"))
		{
			processDelete();
			state = State.DONE;		
		}else
		if(method.equals("POST") && remainingContentLength > 0)
		{
			File blockFile = new File(dataPath + File.separator + blockId + ".block");
			if(!blockFile.exists() && svc.getRemainingSpace() < contentLength)
			{
				writeResponseHeaders("503 No more space", 0);
				state = State.DONE;
			}else
			{
				openOutputDataChannel();
				if(outputDataChannel != null)
				{
					svc.updateRemainingSpace(contentLength);
					remainingContentLength -= buf.remaining();
					outputDataChannel.write(buf);
				}else
				{
					remainingContentLength -=buf.remaining();
					while(buf.hasRemaining()) buf.get();//read and discard
				}
				if(remainingContentLength <= 0)
				{
					state = State.DONE;
					close();
					writeResponseHeaders("200 Ok", 0);				
				}
			}

		}else
		{
			state = State.OUTPUT;
			writeRemaining = false;
			if(method.equals("GET"))
			{
				long dataLen = openInputDataChannel();
				if(inputDataChannel != null)
				{
					writeResponseHeaders("200 Ok", dataLen);
					writeResponseBody();
				}else
				{
					writeResponseHeaders("404 Not Found", 0);
					state = State.DONE;
				}
			}else
			{
				writeResponseHeaders("501 Not Implemented", 0);
				logger.info("Invoking unimplemented method from: {}", remoteAddr);
				state = State.DONE;
			}
		}
		
	}
	
	void processDelete() throws Exception
	{
		String fileid = headerFields.get("X-File-ID");
		if(fileid != null)
		{
			File file;
			if(isInfoBlock)
				file = new File(dataPath + File.separator + blockId + ".iblock" );
			else
				file = new File(dataPath + File.separator + blockId + ".block" );
			
			if(!file.exists())
			{
				writeResponseHeaders("210  Full Delete", 0);
				return;
			}
			
			if(isInfoBlock)
			{
				long fileSize = file.length();
				if(!file.delete())
				{
					System.gc();
					if(!file.delete())
					{
						logger.error("File deletion failed: {}", file);
						writeResponseHeaders("500 Internal Server Error", 0);
					}else
					{
						svc.updateRemainingSpace(-fileSize);					
						writeResponseHeaders("210 Full Delete", 0);											
					}
				}else
				{
					svc.updateRemainingSpace(-fileSize);					
					writeResponseHeaders("210 Full Delete", 0);					
				}
				return;
			}
			
			//we have a data block. we need to update the block meta and then delete the file conditionally.
			
			File path = new File(dataPath);
			File metaPath = new File(dataPath + File.separator + blockId + ".meta");
			BlockMetaInfo meta = BlockMetaInfo.loadIfExists(path, blockId);
			
			meta.getFileids().remove(fileid);

			try
			{
				if(meta.getFileids().size() == 0)					
				{	
					long fileSize = file.length();
					if(!file.delete())
					{
						System.gc(); //To prevent possible JVM quirk. This will GC() all the closed stream objects.
						if(!file.delete())
						{
							logger.error("File deletion failed: {}", file);
						}else
						{
							svc.updateRemainingSpace(-fileSize);
						}
					}else
					{
						svc.updateRemainingSpace(-fileSize);
					}
				
					if(!metaPath.delete())
					{
						System.gc();
						if(!metaPath.delete())
						{
							logger.error("File deletion failed: {}", metaPath);
						}
					}
					if(logger.isDebugEnabled())
						logger.debug("Fully deleting block: {}", blockId);
					writeResponseHeaders("210 Full Delete", 0);//full delete						
				}else
				{
					if(logger.isDebugEnabled())
						logger.debug("Partially deleting block: {}", blockId);					
					writeResponseHeaders("200 Partial Delete", 0);//partial delete						
					meta.store(path, blockId);
				}					
			}catch(Exception e)
			{
				logger.error("Block delete failed: " + blockId, e);
				writeResponseHeaders("500 Internal Server Error", 0);
			}
		}else
		{
			logger.info("Bad Request from: {}", remoteAddr);
			writeResponseHeaders("400 Bad Request", 0);
		}		
	}
	
	Status checkAuth() throws Exception
	{
		String nid = getReqParam("nodeid");
		String data = getBlockId();
		String sign = getHeader("X-Auth-Sign");

		if(sign != null && data != null && nid != null)
			return auth.doAuth(nid, data, sign);
		else
		{
			//TODO log
			return Status.FAILED;
		}
	}
	
	Status fetchAndCheckAuth()
	{
		String nid = getReqParam("nodeid");
		http.setResponseType(HttpClient.ResponseType.XML);
		try
		{
			HttpClient.HttpResponse resp = http.post(props.getProperty("cumulus.server.url", "<not set>"),
				"route", "getnodeinfo",
				"username", props.getProperty("cumulus.username", "<not set>"),
				"secret", props.getProperty("cumulus.secret", "<not set>"),
				"nodeid", nid
					);
		
			if(resp != null && "success".equals(resp.getDocument().getDocumentElement().getAttribute("status")) )
			{
				NodeInfo[] nis = NodeInfo.getNodeInfoArray(resp.getDocument().getDocumentElement());
				if(nis.length > 0)
				{
					auth.addNode(nid,  nis[0].getPublicKey());
					return checkAuth();
				}else
					return Status.FAILED;
			}else
			{
				logger.error("getnodeinfo from server failed for node: " + nid);
				return Status.SERVER_FAILURE;
			}
		}catch(Exception e)
		{
			logger.error("getnodeinfo from server failed (Exception) for node: " + nid, e);
		}
		return Status.SERVER_FAILURE;
		
	}
	
	public void writeResponseHeaders(String responseLine, long contentLength) throws IOException
	{
		this.responseLine = responseLine;
		String response = "HTTP/1.1 " + responseLine + "\r\nContent-Length:" + contentLength + "\r\n\r\n";	
		socketChannel.write(ByteBuffer.wrap((response).getBytes()));
	}
	
	public void writeResponseBody() throws IOException
	{
		if(writeRemaining)
		{
			socketChannel.write(buf);
			if(!buf.hasRemaining())
			{
				writeRemaining = false;
			}
		}
		
		if(!writeRemaining)
		{
		
			buf.clear();
			int n = inputDataChannel.read(buf);
			if(n > 0)
			{
				buf.flip();
				socketChannel.write(buf);
				if(buf.hasRemaining())
				{
					writeRemaining = true;
				}
			}else
			{
				state = State.DONE;
			}
		}

	}

	public void parseHeaderField(String hdr) 
	{
		String[] fields = hdr.split(":");
		
		if(fields.length != 2)
		{
			String n = fields[0].trim();
			StringBuilder v = new StringBuilder();
			for(int i=1;i< fields.length;i++)
			{
				v.append(fields[i].trim());
				if(i < fields.length - 1)
					v.append(':');
			}
			headerFields.put(n, v.toString());
		}else
		{
			String n = fields[0].trim();
			String v = fields[1].trim();
			headerFields.put(n,v);
			if(n.equals("Content-Length"))
			{
				remainingContentLength = Integer.parseInt(v);
				contentLength = remainingContentLength;
			}
		}
	}
		
	
	private void parseRequestLine(String line) throws IOException
	{
		String[] fields = line.split(" ");
		if(fields.length != 3)
		{
			state = State.DONE;
			logger.info("Bad request line: {}", line);
			writeResponseHeaders("400 Bad Request", 0);			
		}else
		{
			method = fields[0].trim();
			reqUrl = fields[1].trim();
			baseUrl = decodeUrl(reqUrl, reqParams);
			httpVer = fields[2].trim();
			if(baseUrl.startsWith("/blocks"))
			{
				blockId = baseUrl.replace("/blocks/", "");
				isInfoBlock = false;
			}else
			if(baseUrl.startsWith("/iblocks"))
			{
				blockId = baseUrl.replace("/iblocks/", "");
				isInfoBlock = true;
			}
		}
	}
	
	public String decodeUrl(String url, HashMap<String,String> params)
	{
		int q = url.indexOf('?');
		if(q < 0)
			return url;
		String baseUrl = url.substring(0, q);
		String sParams = url.substring(q+1);
		String[] nvPairs = sParams.split("&");
		for(String nvPair: nvPairs)
		{
			String[] nv = nvPair.split("=");
			try
			{
				if(nv.length == 2)
				{
					params.put(URLDecoder.decode(nv[0], "UTF-8"), URLDecoder.decode(nv[1], "UTF-8"));
				}else
				{
					//TODO log
				}
			}catch(UnsupportedEncodingException e)
			{
				System.out.println(e);
				//TODO log
			}
		}
		return baseUrl;
	}
			
	private void openOutputDataChannel() throws Exception
	{
		if(outputDataChannel != null) return;
		
		String blockId = getBlockId();
		if("".equals(blockId))
			throw new Exception("Invalid block id");
		
		if(isInfoBlock)
			dataFile = new File(dataPath + File.separator + blockId + ".iblock" );
		else
			dataFile = new File(dataPath + File.separator + blockId + ".block" );
		if(dataFile.createNewFile())
		{
			outputDataStream = new FileOutputStream(dataFile);			
			outputDataChannel = outputDataStream.getChannel();
		}else
		{
			if(dataFile.exists())
			{
				if(isInfoBlock)//overwrite
				{
					outputDataStream = new FileOutputStream(dataFile);			
					outputDataChannel = outputDataStream.getChannel();					
				}
			}
			else
				throw new IOException("Cannot create file: " + dataFile);
		}
	}
	
	public long openInputDataChannel() throws Exception
	{		
		String blockId = getBlockId();
		if("".equals(blockId))
			throw new Exception("Invalid block id");
		
		File file;
		if(isInfoBlock)
			file = new File(dataPath + File.separator + blockId + ".iblock" );
		else
			file = new File(dataPath + File.separator + blockId + ".block" );
		
		if(file.exists())
		{
			inputDataStream = new FileInputStream(file);
			inputDataChannel = inputDataStream.getChannel();
			return file.length();
		}else
			return 0;
	}
		
	public String getMethod()
	{
		return method;
	}
	
	public String getURL()
	{
		return baseUrl;
	}

	public String getFullURL()
	{
		return reqUrl;
	}
	
	public String getBlockId()
	{
		return blockId;
	}
	
	public String getHeader(String name)
	{
		return headerFields.get(name);
	}

	public String getReqParam(String name)
	{
		return reqParams.get(name);
	}
	
	public boolean isOutputMode()
	{
		return state == State.OUTPUT;
	}
	
	public boolean isReadMode()
	{
		return state == State.REQUEST_LINE || state == State.HEADER_FIELDS || state == State.BODY;
	}
	
	public boolean isDone()
	{
		return  state == State.DONE;
	}
	
	public int getContentLength()
	{
		return contentLength;
	}
		
	public File getDataFile()
	{
		return dataFile;
	}
		
	enum State  {REQUEST_LINE, HEADER_FIELDS, BODY, OUTPUT, DONE};
	
	private String responseLine = "";
	
	private State state = State.REQUEST_LINE;
	String method = "";
	private String baseUrl="";
	private String reqUrl = "";
	String httpVer = "";
	private StringBuilder sbuf = new StringBuilder();
	private char prevChar = 0;
	private int crlfs = 0;
	private HashMap<String,String> headerFields = new HashMap<String,String>();
	private HashMap<String,String> reqParams = new HashMap<String,String>();
	int     remainingContentLength=0;	
	int		contentLength = 0;
	String  blockId = "";
	FileChannel outputDataChannel = null;
	FileOutputStream outputDataStream = null;
	FileChannel inputDataChannel = null;
	FileInputStream inputDataStream = null;	
	String dataPath = "";
	SocketChannel socketChannel = null;
	boolean outputMode = true;
	PeerAuthenticator auth;
	File dataFile = null;
	boolean isInfoBlock = false;
	boolean writeRemaining = false;
}
