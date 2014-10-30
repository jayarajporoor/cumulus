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
 * Author: Jayaraj Poroor (jayaraj.poroor@gmail.com)
 * Copyright (c) 2012-2014 Amrita Vishwa Vidyapeetham. All Rights Reserved.
 */

package edu.amrita.selabs.cumulus.lib;

import java.io.BufferedReader;


import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.xml.crypto.dsig.TransformException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class HttpClient {
	
	public enum ResponseType {XML, STRING, STREAM};
	
	int timeOut = 0; 
	HashMap<String, String> headers = new HashMap<String, String>();
	ResponseType responseType = ResponseType.STRING;
	DataOutput responseStream = null;

	Logger logger;
	
	public HttpClient()
	{
		logger = LoggerFactory.getLogger(this.getClass());
	}
	
	public void setResponseStream(DataOutput out)
	{
		this.responseStream  = out;
	}
	
	public void setResponseType(ResponseType t)
	{
		responseType = t;
	}
			
	public void setTimeout(int timeout)
	{
		this.timeOut = timeout;
	}
	
	public void setHeader(String name, String value)
	{
		headers.put(name,  value);
	}
	
	public void clearHeaders()
	{
		headers.clear();
	}

	public HttpResponse delete(String surl, String ... params) throws IOException, MalformedURLException
	{
		return request("DELETE", surl, params);
	}
	
	public HttpResponse get(String surl, String ... params) throws IOException, MalformedURLException
	{
		return request("GET", surl, params);
	}
	
	public HttpResponse post(String surl, String ...params) throws IOException, MalformedURLException
	{
		return request("POST", surl, params);
	}
	
	public HttpResponse request(String method, String surl, String ... params) throws IOException, MalformedURLException
	{
		DataOutputStream dos = null;		
		HttpURLConnection conn = null;
	    
	    String reqString = buildRequest(params);
	    
	    logger.debug("HTTP request. Method:{}, URL: {}", method, surl);
	    
		if(method.equals("GET") || method.equals("DELETE"))
		{
			surl = surl + "?" + reqString;
		}
		URL url = new URL(surl);
		
		try
		{		
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod(method);
		    conn.setConnectTimeout(timeOut * 1000);
		    conn.setReadTimeout(timeOut *1000);
		    conn.setRequestProperty("User-Agent", "Cumulus Client " + StringUtil.getVersion());
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setUseCaches(false);
			if(method.equals("POST"))
			{
				headers.put("Content-Length",   Integer.toString(reqString.length()));
			}
			for(String k: headers.keySet())
			{
				conn.setRequestProperty(k, headers.get(k));
			}
			conn.connect();
			
			if(method.equals("POST"))
			{
				OutputStream os = conn.getOutputStream();
				dos = new DataOutputStream(os);
				dos.writeBytes(reqString);
				dos.flush();
				dos.close();
			}
			
			if(this.responseType == ResponseType.STRING)
			{
				return readResponseAsString(conn);
			}else
			if(this.responseType == ResponseType.XML)
			{
				return readResponseAsXML(conn);
			}else
			{
				return readResponseAsStream(conn);
			}
		}catch(IOException e)
		{
			throw new IOException(e);
		}finally
		{
			FileUtil.close(dos, logger);
			dos = null;
			if(conn != null)
				conn.disconnect();
		}
		
	}
	
	public HttpResponse readResponseAsStream(HttpURLConnection conn) throws IOException
	{
		InputStream is = conn.getInputStream();
		byte [] buf = new byte[1024];
		int len = 0;
		while((len = is.read(buf))> 0)
		{
			responseStream.write(buf, 0, len);
		}
		is.close();
		
		return new HttpResponse("", conn.getResponseCode()); 
	}
	
	public HttpResponse readResponseAsString(HttpURLConnection conn) throws IOException
	{
		InputStream is = conn.getInputStream();
		
	    StringBuilder responseBuf = new StringBuilder();

	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	    String line;
	    while((line = rd.readLine()) != null) {
	       responseBuf.append(line);
	       responseBuf.append('\n');
	    }
	    rd.close();
		String response = responseBuf.toString();
		return new HttpResponse(response, conn.getResponseCode());

	}
	
	public HttpResponse readResponseAsXML(HttpURLConnection conn) throws IOException
	{
		InputStream is = conn.getInputStream();
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
		    builder = builderFactory.newDocumentBuilder();
	        Document document = builder.parse(is);
	        return new HttpResponse(document, conn.getResponseCode());
		} catch (ParserConfigurationException e) {
		    throw new IOException(e);
		}catch(SAXException e)
		{
			throw new IOException(e);
		}
	}

	
	public HttpResponse postFile(File f, String surl, String...params)  throws IOException, MalformedURLException
	{
		FileInputStream in = new FileInputStream(f);
		setHeader("Content-Length",  Long.toString(f.length()));
		return postStream(in, surl, params);
	}

	public HttpResponse postStream(InputStream in, String surl, String ... params) throws IOException, MalformedURLException
	{
		final int RESPONSE_LINE = 0;
		final int HEADER_FIELD = 1;
		final int BODY = 2;
		int responseCode=0;
			
	    StringBuilder responseBuf = new StringBuilder();//TODO response is not filled in now.
	    BufferedReader rd = null;
	    PrintWriter wr = null;
	    OutputStream os = null;
	    InputStream is = null;
	    Socket sock = null;
	    String reqString = buildRequest(params);

		logger.debug("HTTP POST Stream. URL: {}", surl);
		
		URL url = new URL(surl);
		
		try
		{
			InetSocketAddress addr = new InetSocketAddress(url.getHost(), url.getPort());
			sock = new Socket();
			sock.setSoTimeout(timeOut*1000);
			sock.connect(addr, timeOut*1000);
			
			os = sock.getOutputStream();
			wr = new PrintWriter(os);
			wr.print("POST " + url.getPath() +  "?" + reqString + " HTTP/1.0\r\n");
		    wr.print("User-Agent: Cumulus Client" + StringUtil.getVersion() + "\r\n");
		    wr.print("Content-Type: application/binary\r\n");

		    for(String k: headers.keySet())
			{
				wr.print(k);
				wr.print(":");
				wr.print(headers.get(k));
				wr.print("\r\n");
			}
		    wr.print("\r\n");
		    wr.flush();

			byte[] buf = new byte[1024];
			
			int n=0;
			int len = 0;
			
			while((n=in.read(buf))> 0)
			{
				os.write(buf, 0, n);
				os.flush();				
			}

			is = sock.getInputStream();
		    rd = new BufferedReader(new InputStreamReader(is));
		    String line;
		    int state = RESPONSE_LINE;
		    while((line = rd.readLine()) != null) {
		    	line = line.trim();
		    	if(line.equals(""))
		    		break;
		    	switch(state)
		    	{
		    	case RESPONSE_LINE:
		    		String fields[] = line.split(" ");
		    		if(fields.length >= 3)
		    		{
		    			responseCode = Integer.parseInt(fields[1]);
		    		}
		    		state = HEADER_FIELD;
		    		break;
		    	case HEADER_FIELD:
		    		//TODO
		    		break;
		    	default:
		    		break;
		    		
		    	}
		    }

		    os.close();
		    
		    rd.close();
		}catch(IOException e)
		{
			throw new IOException(e);
		}finally
		{
			FileUtil.close(in, logger);
			in = null;
			FileUtil.close(wr, logger);
			wr = null;
			FileUtil.close(rd,  logger);
			rd = null;
			if(sock != null)
				sock.close();
		}

		return new HttpResponse(responseBuf.toString(), responseCode);		
	}
		
	public HttpResponse urlPostStream(InputStream in, String surl, String ... params) throws IOException, MalformedURLException
	{
		HttpURLConnection conn = null;
	    StringBuilder responseBuf = new StringBuilder();
	    BufferedReader rd = null;
	    OutputStream os = null;
	    
	    String reqString = buildRequest(params);
	    
		surl = surl + "?" + reqString;

		URL url = new URL(surl);
		
		try
		{		
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
		    conn.setConnectTimeout(timeOut * 1000);
		    conn.setReadTimeout(timeOut *1000);
		    conn.setRequestProperty("User-Agent", "Cumulus Client " + StringUtil.getVersion());
			conn.setRequestProperty("Content-Type", "application/binary");
			conn.setUseCaches(false);
			for(String k: headers.keySet())
			{
				if(k.equals("Content-Length"))
				{
					int l = Integer.parseInt(headers.get(k));
					conn.setFixedLengthStreamingMode(l);
				}else
					conn.setRequestProperty(k, headers.get(k));
			}
			conn.connect();

			byte[] buf = new byte[1024];
			
			os = conn.getOutputStream();
			int n;

			while((n=in.read(buf)) > 0)
			{
				os.write(buf, 0, n);
				os.flush();
			}

			InputStream is = conn.getInputStream();
		    rd = new BufferedReader(new InputStreamReader(is));
		    String line;
		    while((line = rd.readLine()) != null) {
		       responseBuf.append(line);
		       responseBuf.append('\n');
		    }
			os.close();
		    
		    rd.close();
		}catch(IOException e)
		{
			throw new IOException(e);
		}finally
		{
			if(conn != null)
				conn.disconnect();
			FileUtil.close(in, logger);
			in = null;
			FileUtil.close(rd,  logger);
			rd = null;
			FileUtil.close(os, logger);
			os = null;
		}

		return new HttpResponse(responseBuf.toString(), conn.getResponseCode());		
	}
	
	
	String buildRequest(String ...params) throws MalformedURLException, UnsupportedEncodingException
	{
	    StringBuilder reqStringBuf = new StringBuilder();
		
		boolean isParamName=true;
		boolean isFirstParam = true;
		for(String param: params)
		{
			if(isParamName)
			{
				if(!isFirstParam)
					reqStringBuf.append("&");				
				reqStringBuf.append(param);
				reqStringBuf.append("=");
				isFirstParam = false;
			}else
			{
				reqStringBuf.append(URLEncoder.encode(param, "UTF-8"));
			}
			isParamName  = !isParamName;
		}

		return reqStringBuf.toString();
		
	}
	
	public static class HttpResponse
	{
		String body;
		int    code;
		Document doc;
		
		public HttpResponse(String body, int responseCode)
		{
			this.body = body;
			this.code = responseCode;
			doc = null;
		}
		
		public HttpResponse(Document doc, int responseCode)
		{
			this.doc = doc;
			this.code = responseCode;
		}
		
		public int getCode()
		{
			return code;
		}
		
		public Document getDocument()
		{
			return doc;
		}
		
		public String getBody()
		{
			if(doc != null)
				return docToString(doc);
			else
				return body;
		}
		
		public String toString()
		{
			return getBody();
		}
		
		String docToString(Document document)
		{			
			try
			{
				TransformerFactory transfac = TransformerFactory.newInstance();
				Transformer trans = transfac.newTransformer();
				trans.setOutputProperty(OutputKeys.METHOD, "xml");
				trans.setOutputProperty(OutputKeys.INDENT, "yes");
				trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));

				StringWriter sw = new StringWriter();
				StreamResult result = new StreamResult(sw);
				DOMSource source = new DOMSource(document.getDocumentElement());
				
				trans.transform(source, result);
				return sw.toString();
			}catch(TransformerException e)
			{
				//TODO log
				System.out.println(e);
			}
			return "";			
		}
	}

}
