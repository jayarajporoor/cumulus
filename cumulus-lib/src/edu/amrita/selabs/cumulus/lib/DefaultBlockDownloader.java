package edu.amrita.selabs.cumulus.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.security.Key;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import edu.amrita.selabs.cumulus.lib.DefaultBlockDeleter.DeleteStatus;
import edu.amrita.selabs.cumulus.lib.HttpClient.ResponseType;

public class DefaultBlockDownloader {
	
	
	Properties props;
	Logger logger;
	

	public DefaultBlockDownloader()
	{
		logger = LoggerFactory.getLogger(this.getClass());
	}
	
	public void setProperties(Properties props)
	{
		this.props = props;
	}
	
	public String getBlock(String bid, HttpClient http, RandomAccessFile out, String encodedPrivateKey, GetLog log, File workingFolder, GetLog.GetLogEntry logEntry) throws Exception
	{
		return getBlock(bid, http, out, encodedPrivateKey, log, workingFolder, logEntry, false);
	}
	
	public String getBlock(String bid, HttpClient http, RandomAccessFile out, String encodedPrivateKey, ActionLog log, File workingFolder, GetLog.GetLogEntry logEntry, boolean isInfoBlock) throws Exception
	{
		String userName = props.getProperty("cumulus.username", "");
		String secret = props.getProperty("cumulus.secret", "");
		String nodeid = props.getProperty("cumulus.nodeid", "");
		String serverURL = props.getProperty("cumulus.server.url", "");

		log.refreshProblemNodes(ActionLog.DEFAULT_REFRESH_INTERVAL);
		
		http.clearHeaders();
		http.setResponseType(HttpClient.ResponseType.XML);
		
		String type = "block";
		
		if(isInfoBlock)
			type = "iblock";
		
		HttpClient.HttpResponse resp = http.post(serverURL,
												"route", "getblockinfo",
												"username", userName, 
												"secret", secret,
												"blockid", bid,
												"aliveonly", "true",
												"type", type);

		Document doc  = resp.getDocument();
		if("success".equals(doc.getDocumentElement().getAttribute("status")))
		{
			boolean noNodesAvailable = true;
			
			NodeInfo[] nis =  NodeInfo.getNodeInfoArray(doc.getDocumentElement());
			for(NodeInfo n: nis)
			{
				if(logEntry.getBadNodes().contains(n.getId()))
				{
					logger.info("Skipping bad node: {} for block: {}", n.getId(), bid);
					continue;
				}
				
				if(log.getProblemNodes().containsKey(n.getId()))
				{
					logger.info("Skipping problem node: {}, IP: {} which has not responded recently.", n.getId(), n.getIp());
					continue;
				}
				noNodesAvailable = false;
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

				http.clearHeaders();
				http.setHeader("X-Auth-Sign",  sign);
	
				String f;
				if(isInfoBlock)
					f = "/iblocks/";
				else
					f = "/blocks/";
				String url = "http://" + n.getIp() + ":" + n.getPort() + f + bid;
				
				out.seek(0);
				http.setResponseType(ResponseType.STREAM);
				http.setResponseStream(out);
				resp = null;
				try
				{
					resp = http.get(url,  "nonce", "123456", "nodeid", nodeid);
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
						log.store(workingFolder);
						if(logger.isDebugEnabled())
							logger.debug("Adding problem node: {}, IP: {}", n.getId(), n.getIp());						
					}
					logger.info("Node I/O failed. Continuing. Nodeid: " + n.getId() + ", IP: " + n.getIp(), e);
				}

				if(resp != null && log.getProblemNodes().containsKey(n.getId()))
				{
					log.getProblemNodes().remove(n.getId());
					log.store(workingFolder);
				}
				if(resp != null && resp.getCode() == 200)
					return n.getId();
				
			}
			if(logEntry.isNoNodesAvailable() != noNodesAvailable)
			{
				logEntry.setNoNodesAvailable(noNodesAvailable);
				log.store(workingFolder);
			}
		}else
		{
			throw new IOException(doc.getDocumentElement().getTextContent());
		}
		
		return null;
	}
}
