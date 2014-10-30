package edu.amrita.selabs.cumulus.node.test;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioTest {
	public static void main(String [] args) throws Exception
	{
		ServerSocketChannel server = ServerSocketChannel.open();
		server.socket().bind(new java.net.InetSocketAddress(9090));			
		
		Selector selector = Selector.open();
		server.configureBlocking(false);
		server.register(selector,  SelectionKey.OP_ACCEPT);
		
		System.out.println("Waiting for connections....");
		while(true)
		{
			selector.select();
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> it = keys.iterator();

			while(it.hasNext())
			{
				SelectionKey k = it.next();
				it.remove();
				SelectableChannel ch = k.channel();				
				if(k.isAcceptable())
				{
					SocketChannel client = server.accept();
					client.configureBlocking(false);
					client.register(selector,  SelectionKey.OP_READ, ByteBuffer.allocate(1024));
					System.out.println("Accepting new connection from : " + client.socket().getRemoteSocketAddress());
				}else
				if(k.isReadable())
				{
					SocketChannel sch = (SocketChannel) ch;
					ByteBuffer buf = (ByteBuffer)k.attachment();
					buf.clear();
					int n = sch.read(buf);
					if(n > 0)
					{
						buf.flip();
						while(buf.hasRemaining())
							buf.get();
						//System.out.println("Got data: " + n);
						//TODO
					}else
					if(n < 0)
					{
						System.out.println("Closing channel: " + sch.socket().getRemoteSocketAddress());
						sch.close();
						k.cancel();
					}else
					{
						System.out.println("Zero read");
					}
				}
				
			}
		}
		

	}
}
