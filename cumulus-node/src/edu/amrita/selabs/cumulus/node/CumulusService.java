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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.amrita.selabs.cumulus.lib.HttpClient;
import edu.amrita.selabs.cumulus.lib.RSAKeyUtil;
import edu.amrita.selabs.cumulus.lib.StringUtil;
import edu.amrita.selabs.cumulus.lib.FileUtil;
import edu.amrita.selabs.cumulus.lib.XmlUtil;

public class CumulusService implements Runnable{
	public static final long MAX_SHUTDOWN_TIME_MS = 10 * 1000;
	
	DefaultPeerAuthenticator auth = new DefaultPeerAuthenticator();
	final ServerSocketChannel server;
	final DatagramChannel udp;	
	final   HashSet<HttpProcessor> activeProcessors = new HashSet<HttpProcessor>();
	final Properties props;
	final Logger logger;
	long  remainingSpace = 0;
	File spaceFile;
	int   keepalivePeriod = 30; //seconds
	
	
	public CumulusService(Properties props) throws Exception
	{
		server = ServerSocketChannel.open();
		udp    =  DatagramChannel.open();
		logger = LoggerFactory.getLogger(this.getClass());
		this.props = props;
		try
		{
			File metaFolder = new File(props.getProperty("cumulus.node.metafolder", ""));
			if(metaFolder.exists() && metaFolder.isDirectory())
			{
				spaceFile = new File(metaFolder.getAbsolutePath() + File.separator + ".cumulus.avail");
				if(!spaceFile.exists())
				{
					throw new IOException("Remaining space file does not exist: " + spaceFile.getAbsolutePath());
				}
				remainingSpace = Long.parseLong(FileUtil.getFileAsString(spaceFile).trim());
			}else
				throw new IOException("cumulus.node.metafolder does not refer to a folder.");

		}catch(Exception e)
		{
			logger.error("Cannot start cumulus service. Exception", e);
			throw e;
		}
		
		try
		{
			keepalivePeriod = Integer.parseInt(props.getProperty("cumulus.node.keepalive", "30")); 
		}catch(Exception e)
		{
			logger.error("Invalid cumulus.node.keepalive parameter. Defaulting to: {}", keepalivePeriod);
		}
		
		File dataPath = new File(props.getProperty("cumulus.node.datapath"), "");
		if(!dataPath.exists() || !dataPath.isDirectory())
		{
			throw new IOException("Data path (cumulus.node.datapath property): " + dataPath.getAbsolutePath() + " is not a directory.");
		}
	}
	
	public void installShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(
				new Thread()
				{
					public void run()
					{
						
						long t= 0;
						logger.info("Shutting down. Waiting for active HTTP processors to close...");
						try
						{
							server.close(); //prevent new requests.
							udp.close();
							while(!activeProcessors.isEmpty())
							{
								try
								{
									Thread.sleep(500);
								}catch(InterruptedException e)
								{
									logger.error("Thread.sleep interrupted", e);
								}
								t += 500;
								if(t > MAX_SHUTDOWN_TIME_MS)
								{
									logger.error("Active HTTP processors present: {} - still forcing shutdown", activeProcessors.size());
									break;
								}
							}
						}catch(Exception e)
						{
							System.out.println(e);
						}
						logger.info("Shutdown completed.");
						
					}
				}
			);
		
	}
	
	public void startKeepalive(final int period)
	{
		Runnable keepalive = new Runnable()
		{
			HttpClient http = new HttpClient();

			public  void run()
			{
				String serverURL = props.getProperty("cumulus.server.url", "");
				String username = props.getProperty("cumulus.username", "");
				String secret = props.getProperty("cumulus.secret", "");
				String nodename = props.getProperty("cumulus.nodename");

				http.setResponseType(HttpClient.ResponseType.XML);
				
				while(true)
				{
					try
					{
						Thread.sleep(1000*period);
					}catch(InterruptedException e)
					{
						//ignore
					}
					
					try
					{
						HttpClient.HttpResponse resp = http.post(serverURL, "route", "keepalive", 
								"username", username, 
								"secret", secret,
								"nodename", nodename);
						
						if(resp != null && "success".equals(resp.getDocument().getDocumentElement().getAttribute("status")))
						{
							logger.debug("Keepalive successful");
						}else
						{
							String desc;
							if(resp == null)
								desc = "null response";
							else
								desc = resp.getDocument().getDocumentElement().getTextContent();
							logger.error("Keepalive failed: {}", desc);
						}
					}catch(Exception e)
					{
						logger.error("Exception when attempting keepalive with the coordinator. ", e);
					}
				}
			}
		};
		new Thread(keepalive).start();
	}
	
	public void run()
	{
		assert(props != null);
		
		installShutdownHook();
		startKeepalive(keepalivePeriod);
		serverLoop();
	}
	
	public void serverLoop()
	{
		String dataPath  = props.getProperty("cumulus.node.datapath", "");
		try
		{						
			ByteBuffer buf = ByteBuffer.allocate(1024);
	
			byte[] b = new byte[1024];

			String s = "";
			
			Selector selector = Selector.open();
		
			/*TODO: if we need to support UDP later on:
			udp.socket().bind(new InetSocketAddress(9090));
			udp.configureBlocking(false);			
			udp.register(selector,SelectionKey.OP_READ);
			*/
			
			server.socket().bind(new java.net.InetSocketAddress(9090));			
			server.configureBlocking(false);			
			server.register(selector,  SelectionKey.OP_ACCEPT);
			
			logger.info("Starting Cumulus Service: {} with remaining space: {}", StringUtil.getVersion(), remainingSpace);
	
			do
			{
				selector.select();
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();

				while(it.hasNext())
				{
					SelectionKey k = null;
					k = it.next();
					it.remove();						

					buf.clear();					
					if(k.isAcceptable())
					{
						SelectableChannel ch = k.channel();
						if(ch == server)
						{
							SocketChannel client = server.accept();
							if(client != null)
							{
								logger.info("Accepting new connection from: ", client.socket().getRemoteSocketAddress());
								client.configureBlocking(false);
								HttpProcessor proc = new HttpProcessor(dataPath, client, auth, new HttpClient(), props,
										client.socket().getRemoteSocketAddress().toString(), this);
								client.register(selector,  SelectionKey.OP_READ, proc);
								activeProcessors.add(proc);
							}else
							{
								logger.error("accept() returned null. Continuing.");
							}
						}else
						{
							logger.error("Acceptible socket channel is not the server channel. Ignoring.");
						}
					}else
					if(k.isReadable())
					{
						SelectableChannel ch =  k.channel();
						if(ch instanceof DatagramChannel)
						{
							DatagramChannel dch = (DatagramChannel) ch;
							InetSocketAddress remoteAddr = (InetSocketAddress) dch.receive(buf);
							buf.flip();
							buf.get(b, 0, buf.limit());
							//TODO (if we require UDP processing later on)
						}else
						{
							SocketChannel sch = (SocketChannel) ch;
							HttpProcessor proc = (HttpProcessor) k.attachment();
							try
							{
								ByteBuffer rbuf = proc.getBuf();
								rbuf.clear();
								int n = sch.read(rbuf);
								if( n > 0)
								{
									rbuf.flip();
									if(proc.isReadMode())
									{
										proc.process();
									}
									if(proc.isOutputMode())
									{
										k.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
									}else
									if(proc.isDone())
									{
										activeProcessors.remove(proc);										
										try
										{
											if(sch.isConnected() && sch.socket().isConnected())
												sch.socket().shutdownOutput();
										}catch(SocketException e)
										{
											logger.error("Cannot shutdown socket", e);
										}
									}
								}else
								if(n < 0)
								{
									proc.close();
									activeProcessors.remove(proc);
									logger.info("Closing channel with remote address: {}. HTTP Processor state: {}", 
											sch.socket().getRemoteSocketAddress(), proc.getState().name());
									sch.close();
									k.cancel();
								}
							}catch(Exception e)
							{
								proc.close();								
								activeProcessors.remove(proc);
								sch.close();
								k.cancel();
								logger.error("Exception", e);
							}
						}
					}else
					if(k.isWritable())
					{
						SocketChannel sch =  (SocketChannel)k.channel();						
						HttpProcessor proc = (HttpProcessor) k.attachment();
						if(proc.isOutputMode())
						{
							proc.writeResponseBody();
						}else
						{
							k.interestOps(SelectionKey.OP_READ);
						}
						if(proc.isDone())
						{
							activeProcessors.remove(proc);							
							try
							{
								if(sch.isConnected() && sch.socket().isConnected())
									sch.socket().shutdownOutput();
							}catch(SocketException e)
							{
								logger.error("Cannot shutdown socket", e);
							}
						}
					}
				}
			}while(true);
		}catch(Exception e)
		{
			System.out.println(e);
			e.printStackTrace();
		}		
	}
	
	public void commitRemainingSpace()
	{
		PrintWriter wr = null;
		try
		{
			wr = new PrintWriter(new FileOutputStream(spaceFile));
			wr.print(Long.toString(remainingSpace));
		}catch(IOException e)
		{
			logger.error("Cannot commit remaining space to file " + spaceFile);
		}finally
		{
			FileUtil.close(wr, logger);
			wr = null;
		}
	}
	
	public long getRemainingSpace()
	{
		return remainingSpace;
	}
	
	public void updateRemainingSpace(long v)
	{
		remainingSpace -= v;
		commitRemainingSpace();
	}
		
	public void createNode() throws Exception
	{
		File keyFolder = new File(props.getProperty("cumulus.keyfolder", ""));
		
		if(!keyFolder.exists())
		{
			if(!keyFolder.mkdir())
				throw new IOException("Cannot create folder: " + keyFolder.getAbsolutePath());
		}
		
		if(!keyFolder.isDirectory())
		{
			throw new IOException("cumulus.keyfolder property: "  + keyFolder + " is not a directory");
		}

		File metaFolder = new File(props.getProperty("cumulus.node.metafolder", ""));
		
		if(!metaFolder.exists())
		{
			if(!metaFolder.mkdir())
				throw new IOException("Cannot create folder: " + metaFolder.getAbsolutePath());
		}
		
		if(!metaFolder.isDirectory())
		{
			throw new IOException ("cumulus.node.metafolder property: " + metaFolder + " is not a directory");
		}
		
		File cumulusPub = new File(keyFolder.getAbsolutePath() + File.separator + "cumulus.pub");
		File cumulusPriv = new File(keyFolder.getAbsolutePath() + File.separator + "cumulus.priv");
		if(cumulusPub.exists())
			throw new IOException(cumulusPub + " Exists.");
		if(cumulusPriv.exists())
			throw new IOException(cumulusPriv + " Exists.");
		
		
		RSAKeyUtil keyUtil = new RSAKeyUtil();
		HttpClient http = new HttpClient();
		
		keyUtil.genKeyPair();
		
		String sPubKey = keyUtil.getEncodedPublicKey();
		String sPrivKey = keyUtil.getEncodedPrivateKey();
		
		PrintWriter wr = null;
		
		try
		{
			wr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cumulusPub)));
			
			wr.println(sPubKey);
			
			wr.close();
			
			wr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cumulusPriv)));
			
			wr.println(sPrivKey);
			
			wr.close();
			
			http.setResponseType(HttpClient.ResponseType.XML);//TODO: actually XML - but we will add the parsing logic later.
			HttpClient.HttpResponse res = http.post(props.getProperty("cumulus.server.url", ""),
					"route", "create_node",
					"username", props.getProperty("cumulus.username", ""),
					"secret",props.getProperty("cumulus.secret", ""),
					"nodename", props.getProperty("cumulus.nodename", ""),
					"avail_space", props.getProperty("cumulus.node.totalspace", ""),
					"publickey", sPubKey,
					"ipaddr", props.getProperty("cumulus.node.ipaddr", ""),
					"port", props.getProperty("cumulus.node.port", ""));
	
			File spaceFile = new File(metaFolder.getAbsolutePath() + File.separator + ".cumulus.avail");
			wr = new PrintWriter(new FileOutputStream(spaceFile));
			wr.print(props.getProperty("cumulus.totalspace", "0"));
			wr.flush();
			wr.close();

			PrintWriter writer = new PrintWriter(new FileOutputStream(".cumulus.create.xml"));
			XmlUtil.printXml(res.getDocument(), writer);
			
			if("success".equals(res.getDocument().getDocumentElement().getAttribute("status")))
			{
				logger.info("Node creation succeeded");
			}else
			{
				logger.error("Node Creation failed: See .cumulus.create.xml");
			}
		}finally
		{
			if(wr != null)
				wr.close();
		}
		
	}
	
	
	public static void main(String[] args) throws Exception
	{
		Properties props = new Properties();
		if(args.length > 0)
		{
			FileInputStream in = new FileInputStream(args[0]);
			props.load(in);
			in.close();
		}else
		{
			//TODO - remove - for unit testing purposes.
			props.setProperty("cumulus.node.datapath", "/tmp");
			props.setProperty("cumulus.username", "test");
			props.setProperty("cumulus.secret",  "ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae");
			props.setProperty("cumulus.server.url", "http://localhost/cumulus/cumulus-svr/router.php");
		}
		
		CumulusService cumulus = new CumulusService(props);		

		if(args.length > 1 && args[1].equals("createnode"))
		{
			cumulus.createNode();
		}else
		{
			cumulus.run();
		}
	}
}
