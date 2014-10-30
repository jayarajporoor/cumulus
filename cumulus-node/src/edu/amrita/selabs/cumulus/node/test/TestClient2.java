package edu.amrita.selabs.cumulus.node.test;

import java.io.File;
import java.io.FileInputStream;

public class TestClient2 {
	public static void main(String[] args) throws Exception
	{
		int timeOut = 0;
		if(args.length < 2) 
		{
			System.out.println("Usage <ip-addr> <file>");
			return;
		}
		
		FileInputStream in = new FileInputStream(args[1]);
		long len = new File(args[1]).length();
		System.out.println("Length: " + len);
		do
		{
			
		}while(len > 0);

	}
}
