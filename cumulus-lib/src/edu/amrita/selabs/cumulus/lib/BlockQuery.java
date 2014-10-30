package edu.amrita.selabs.cumulus.lib;

public class BlockQuery {
	
	public static final int DEFAULT_UDP_PORT = 9090;
	
	public void setUdpPort(int port)
	{
		this.udpPort = port;
	}
	
	
	int udpPort = DEFAULT_UDP_PORT;
}
