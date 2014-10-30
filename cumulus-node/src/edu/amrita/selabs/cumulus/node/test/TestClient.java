package edu.amrita.selabs.cumulus.node.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TestClient {
	public static void main(String[] args) throws Exception
	{
		int timeOut = 0;
		if(args.length < 2) 
		{
			System.out.println("Usage <ip-addr> <file>");
			return;
		}

		InetSocketAddress addr = new InetSocketAddress(args[0], 9090);
		
		long len = new File(args[1]).length();
		System.out.println("Length: " + len);
		int k =0;
		FileInputStream in = new FileInputStream(args[1]);
		
		do
		{
			k++;
			Socket sock = new Socket();
			sock.setSoTimeout(timeOut*1000);
			System.out.println("Connecting...");
			sock.connect(addr, timeOut*1000);
		
			OutputStream os = sock.getOutputStream();
			PrintWriter wr = new PrintWriter(os);
			
			int n;
			byte buf[] = new byte[1024];
			int blockSize = 1024* 512;
			String blockid = "ABCD" + k;
			wr.print("POST /blocks/" + blockid + " HTTP/1.1\r\n");
			long contentLength = len > blockSize ? blockSize : len;
			
			wr.print("Content-Length:" + contentLength + "\r\n");
			wr.print("Content-Type: application/binary\r\n");
			wr.print("X-File-ID: 1234\r\n");
			wr.print("\r\n");
			wr.flush();
			while((n = in.read(buf)) > 0)
			{
				os.write(buf, 0, n);
				os.flush();
				len -= n;
				blockSize -= n;
				if(blockSize <= 0 || len <= 0)
					break;
			}
			os.close();
			in.close();
		}while(len > 0);
	}
}
