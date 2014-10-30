package edu.amrita.selabs.cumulus.lib;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeInfo
{
	String id;
	String ip;
	String publickey;
	int port;
	
	public NodeInfo(String id, String ip, String publickey, int port)
	{
		this.id = id;
		this.ip = ip;
		this.publickey = publickey;
		this.port = port;
	}
	
	public String getId()
	{
		return id;
	}
	
	public String getIp()
	{
		return ip;
	}
	
	public String getPublicKey()
	{
		return publickey;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public String toString()
	{
		return id + "@" + ip + ":" + port + ';' + publickey;
	}

	public static NodeInfo[] getNodeInfoArray(Element parent)
	{
		NodeList nodes = parent.getChildNodes();
		int k = 0;
		for(int i=0;i< nodes.getLength();i++)
		{
			Node n = nodes.item(i);
			if(n instanceof Element && ((Element)n).getNodeName().equals("node"))
			{
				k++;
			}
		}
		
		NodeInfo[] ni = new NodeInfo[k];
		
		k=0;
		
		for(int i=0;i< nodes.getLength();i++)
		{
			Node n = nodes.item(i);
			if(n instanceof Element)
			{
				Element e = (Element) n;
				if(e.getNodeName().equals("node"))
				{
					String id = XmlUtil.getChildAsString(e, "id");
					String ip = XmlUtil.getChildAsString(e, "ip");
					String publickey = XmlUtil.getChildAsString(e, "publickey");
					String sport = XmlUtil.getChildAsString(e, "port");						
					int port = Integer.parseInt(sport);
					ni[k] = new NodeInfo(id, ip, publickey, port);
					k++;					
				}
			}
		}
		
		return ni;
		
	}
}
