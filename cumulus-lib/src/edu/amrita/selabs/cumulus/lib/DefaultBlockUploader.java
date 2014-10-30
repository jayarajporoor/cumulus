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
 *
 * Author: Jayaraj Poroor (jayaraj.poroor@gmail.com)
 */

package edu.amrita.selabs.cumulus.lib;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.amrita.selabs.cumulus.lib.HttpClient.ResponseType;

public class DefaultBlockUploader implements BlockUploader{
	
	Properties props;
	HttpClient http = new HttpClient();
	
	Logger logger;
	

	public DefaultBlockUploader()
	{
		logger = LoggerFactory.getLogger(this.getClass());
	}
	
	
	public void setProperties(Properties props)
	{
		this.props = props; 
	}
	
	public void putBlock(String bid, String fileid, File blockFile, long blockSize, String encodedPrivateKey, PutLog.PutLogEntry logEntry, PutLog log, File workingFolder) throws IOException
	{
		putBlock(bid, fileid, blockFile, blockSize, encodedPrivateKey, logEntry,  log, workingFolder, false);
	}
	
	public void putBlock(String bid, String fileid, File blockFile, long blockSize, 
									String encodedPrivateKey, PutLog.PutLogEntry logEntry, PutLog log, File workingFolder, boolean isInfoBlock) throws IOException
	{
		String serverURL 	= props.getProperty("cumulus.server.url", "");
		String username 	= props.getProperty("cumulus.username", "");
		String secret   	= props.getProperty("cumulus.secret", "");
		String nodeid   	= props.getProperty("cumulus.nodeid", "");
		
		log.refreshProblemNodes(ActionLog.DEFAULT_REFRESH_INTERVAL);
		
		NodeInfo[] nodeInfo = getNodes(bid, blockSize, logEntry.getAlreadyPut(), log.getProblemNodes().keySet()); 
	
		RSAKeyUtil keyUtil = new RSAKeyUtil();

		String sign;
		try
		{
			Key privKey = keyUtil.decodePrivateKey(encodedPrivateKey);
		
			sign = new RSACryptoUtil().sign(bid,  privKey);
		}catch(Exception e)
		{
			throw new IOException(e);
		}
		
		FileInputStream blockStream = null;
		
		for(NodeInfo n: nodeInfo)
		{			
			http.setResponseType(ResponseType.STRING);
			
			http.clearHeaders();

			http.setHeader("X-Auth-Sign",  sign);
			http.setHeader("X-File-ID", fileid);
			http.setHeader("Content-Length", Long.toString(blockSize));
			
			String remoteFolder;
			if(isInfoBlock)
				remoteFolder = "/iblocks/";
			else
				remoteFolder = "/blocks/";
			String url = "http://" + n.getIp() + ":" + n.getPort() + remoteFolder + bid;
			
			try
			{
				blockStream = new FileInputStream(blockFile);
				http.setResponseType(HttpClient.ResponseType.STRING);

				HttpClient.HttpResponse resp=null;
				try
				{
					resp = http.postStream(blockStream,  url,  "nonce", "123456", "nodeid", nodeid);
				}
				catch(IOException e)
				{
					String cause=null;
					if(e instanceof ConnectException || e.getCause() instanceof ConnectException)
					{
						cause = "connect";
					}else
					if(e instanceof NoRouteToHostException || e.getCause() instanceof NoRouteToHostException)
					{
						cause = "noroute";						
					}
					if(cause != null)
					{
						log.getProblemNodes().put(n.getId(), new ActionLog.ProblemNodeEntry(cause, System.currentTimeMillis()));
						if(logger.isDebugEnabled())
							logger.debug("Adding problem node: {}, IP: {}", n.getId(), n.getIp());
						log.store(workingFolder);						
					}
				
					logger.info("Node I/O failed. Continuing. Nodeid: " + n.getId() + ", IP: " + n.getIp(), e);
				}

				if(resp != null && log.getProblemNodes().containsKey(n.getId()))
				{
					log.getProblemNodes().remove(n.getId());
					log.store(workingFolder);
				}
				
				if(resp != null && resp.getCode() == 200)
				{
					logger.debug("Uploaded blockid: {} to node {}, ip:{}", bid, n.getId(), n.getIp());
					if(!logEntry.getAlreadyPut().contains(n.getId()))
					{
						logEntry.getAlreadyPut().add(n.getId());					
					}

					if(!logEntry.getToNotifyServer().contains(n.getId()))
					{					
						logEntry.getToNotifyServer().add(n.getId());									
					}
					
					log.store(workingFolder);
					
					String type = "block";
					if(isInfoBlock)
						type = "iblock";
					http.setResponseType(HttpClient.ResponseType.XML);
	
					resp = http.post(serverURL,  "route", "putblockinfo", 
									  	"username", username, 
									  	"secret", secret, 
									  	"type", type,
									  	"blockid", bid,
									  	"nodeid", n.getId(),
									  	"blocksize", Long.toString(blockSize)
									  	);
					
					if(resp.getCode() == 200 && "success".equals(resp.getDocument().getDocumentElement().getAttribute("status")))
					{
						logEntry.getToNotifyServer().remove(n.getId());
						log.store(workingFolder);
					}
				}else
				{
					int respCode = 0;
					if(resp != null)
						respCode = resp.getCode();
					logger.info("Node I/O failed. Continuing. Nodeid: {}, IP: {}, Reason: {}", n.getId(),  n.getIp(),  respCode);					
				}
				
			}catch(Exception e)
			{
				logger.info("Node I/O failed. Continuing. Nodeid: " + n.getId() + ", IP: " + n.getIp(), e);
			}finally
			{
				if(blockStream != null)
				{
					blockStream.close();
					blockStream = null;
				}
			}
		}
	}
	
	NodeInfo[] getNodes(String bid, long blockSize, HashSet<String> nodeids, Set<String> problemNodes) throws IOException
	{
		String serverURL 	= props.getProperty("cumulus.server.url", "");
		String username 	= props.getProperty("cumulus.username", "");
		String secret   	= props.getProperty("cumulus.secret", "");
		String nodename   	= props.getProperty("cumulus.nodename", "");
		int replicationFactor = Integer.parseInt(props.getProperty("cumulus.replication.factor", "1"));
		int need			= replicationFactor - nodeids.size();
		
		if(need <= 0)
			return new NodeInfo[0];
		
		String dontuse;
		if(nodeids.size() > 0)
		{
			if(problemNodes.size() > 0)
				dontuse = StringUtil.implode(",", nodeids) + "," + StringUtil.implode(",", problemNodes);
			else
				dontuse = StringUtil.implode(",", nodeids);
		}else
		{
			if(problemNodes.size() > 0)
				dontuse = StringUtil.implode(",", problemNodes);
			else
				dontuse = "";
		}
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Requesting getnodes from server: {}. dontuse: {}", serverURL, dontuse);
		}
		http.setResponseType(HttpClient.ResponseType.XML);
		
		HttpClient.HttpResponse resp = http.post(serverURL, "route", "getnodes", 
												"username", username, 
												"secret", secret,
												"nodename", nodename,
												"blockid", bid,
												"blocksize", Long.toString(blockSize),
												"dontuse", dontuse,
												"need", Integer.toString(need));

		Document doc  = resp.getDocument();
		if(doc.getDocumentElement().getAttribute("status").equals("success"))
		{
			return  NodeInfo.getNodeInfoArray(doc.getDocumentElement());
		}else
		{
			throw new IOException("Server returned error: " + doc.getDocumentElement().getTextContent());
		}

		
	}
		
}
