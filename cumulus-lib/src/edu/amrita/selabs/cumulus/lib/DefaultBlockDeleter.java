package edu.amrita.selabs.cumulus.lib;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.security.Key;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import edu.amrita.selabs.cumulus.lib.HttpClient.ResponseType;

public class DefaultBlockDeleter {
	
	enum DeleteStatus {PARTIAL_DELETE, FULL_DELETE, ERROR, NODE_NOT_AVAILABLE};
	
	Properties props;
	
	Logger logger;
	

	public DefaultBlockDeleter()
	{
		logger = LoggerFactory.getLogger(this.getClass());
	}
	
	
	public void setProperties(Properties props)
	{
		this.props = props;
	}

	public DeleteStatus deleteBlock(File workingFolder, HttpClient http, String nodeid, String blockid, String fileid, DeleteLog log, String encodedPrivateKey) throws Exception
	{
		return deleteBlock(workingFolder, http, nodeid, blockid, fileid, log, encodedPrivateKey, false);
	}

	public DeleteStatus deleteBlock(File workingFolder, HttpClient http, String nodeid, String blockid, String fileid, DeleteLog log, String encodedPrivateKey, boolean isInfoBlock) throws Exception
	{
		String userName = props.getProperty("cumulus.username", "");
		String secret = props.getProperty("cumulus.secret", "");
		String myNodeid = props.getProperty("cumulus.nodeid", "");
		String serverURL = props.getProperty("cumulus.server.url", "");
		
		log.refreshProblemNodes(ActionLog.DEFAULT_REFRESH_INTERVAL);
		
		if(log.getProblemNodes().containsKey(nodeid))
		{
			logger.info("Deferring delete block for blockid:{} from nodeid: {} since this node has not responded recently", blockid, nodeid);
			return DeleteStatus.NODE_NOT_AVAILABLE;
		}
		http.setResponseType(HttpClient.ResponseType.XML);
		http.clearHeaders();		
		HttpClient.HttpResponse resp = http.post(serverURL,
												"route", "getnodeinfo",
												"username", userName, 
												"secret", secret,
												"nodeid", nodeid);

		Document doc  = resp.getDocument();
		if("success".equals(doc.getDocumentElement().getAttribute("status")))
		{
			NodeInfo[] nis =  NodeInfo.getNodeInfoArray(doc.getDocumentElement());
			if(nis.length > 0)
			{
				//we will consider only the first element.
				NodeInfo n = nis[0];
				
				RSAKeyUtil keyUtil = new RSAKeyUtil();

				String sign;
				try
				{
					Key privKey = keyUtil.decodePrivateKey(encodedPrivateKey);
				
					sign = new RSACryptoUtil().sign(blockid,  privKey);
				}catch(Exception e)
				{
					throw new IOException(e);
				}
				http.clearHeaders();				
				http.setHeader("X-Auth-Sign",  sign);
				http.setHeader("X-File-ID", fileid);
				
				String f;
				if(isInfoBlock)
					f = "/iblocks/";
				else
					f = "/blocks/";
				
				String url = "http://" + n.getIp() + ":" + n.getPort() + f + blockid;
				
				http.setResponseType(ResponseType.STRING);
				resp = null;
				try
				{
					resp = http.delete(url,  "nonce", "123456", "nodeid", myNodeid);
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

				if(resp != null)
				{
					logger.debug("Deleted block:{} from node:{}, IP:{}", blockid, n.getId(), n.getIp());
					if(resp.getCode() == 210 || resp.getCode() == 404)
						return DeleteStatus.FULL_DELETE;
					else
					if(resp.getCode() == 200)
						return DeleteStatus.PARTIAL_DELETE;
				}else
				{
					logger.error("Node I/O failed. Null Response. Continuing. Nodeid: {}, IP: {}", n.getId(),  n.getIp());					
				}
			}
		}else
		{
			logger.error("Node: {} not found on the coordinator", nodeid);
		}
		return DeleteStatus.ERROR;
		
	}
	

}
