package edu.amrita.selabs.cumulus.lib;

import java.io.BufferedReader;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;

public class SimpleCLI {
	
	public static void main(String [] args) throws Exception
	{
		Properties props = new Properties();;
		if(args.length > 0)
		{
			FileInputStream in = new FileInputStream(args[0]);
			props.load(in);
			in.close();
		}else
		{
			System.out.println("Give properties file as argument");
			return;
		}
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProgressCallback(
			new ProgressCallback()
			{
				public void taskBegin(String name)
				{
					System.out.print("Starting " + name);
					System.out.flush();
				}
				
				public void taskEnd()
				{
					System.out.println(" Completed.");
					System.out.flush();					
				}
				
				public void taskError(String desc)
				{
					System.out.println(" Task completed with errors: " + desc + " See log for more details.");
					System.out.flush();					
				}
				
				public void taskProgress(String desc)
				{
					System.out.print(".");
					System.out.flush();					
				}
				
				public void taskPartEnd()
				{
					System.out.println(" Partially completed.");
					System.out.flush();					
				}

			}
		);		
		dcc.setProperties(props);
		
		int replicationFactor = Integer.parseInt(props.getProperty("cumulus.replication.factor", "1"));
		int blockSize = Integer.parseInt(props.getProperty("cumulus.blocksize", "" + dcc.minBlockSize));
		BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));

		while(true)
		{
			System.out.print("cumulus:>");
			String line = rd.readLine();
			String[] fields = line.split(" ");
			if("exit".equals(fields[0]))
			{
				System.out.println("Bye");
				return;
			}else
			if("put".equals(fields[0]))
			{
				File file = new File(fields[1]);
				PutLog log = dcc.put(fields[1]);
				if(!log.fibDone(replicationFactor) || !log.allBlocksDone(replicationFactor, blockSize, file.length()))
				{
					System.out.println("put operation has only partially completed. Please rerun after some time to complete.");
				}
			}else
			if("get".equals(fields[0]))
			{
				if(fields.length < 2)
				{
					System.out.println("Usage: get <filename> [<targetfoldername>]");
				}else
				{
					String folder = ".";
					if(fields.length >= 3)
						folder = fields[2];
				
					GetInfo gInfo = dcc.get(fields[1], folder);
					if(gInfo.getStatus() == GetInfo.Status.IN_PROGRESS)
					{
						System.out.println("get operation has only partially completed. Please rerun after some time to complete.");
					}else
					if(gInfo.getStatus() == GetInfo.Status.FILE_NOT_FOUND)
					{
						System.out.println("File not found in the Cumulus system");
					}else
					if(gInfo.getStatus() == GetInfo.Status.COMBINE_FAILED)
					{
						System.out.println("Combine phase failed to build the target file. See log for details.");
					}
				}

			}else
			if("delete".equals(fields[0]))
			{
				if(fields.length >= 2)
				{
					DeleteInfo dInfo = dcc.delete(fields[1]);
					if(dInfo.getStatus() == DeleteInfo.Status.IN_PROGRESS)
					{
						System.out.println("delete operation has only partially completed. Please rerun after some time to complete.");
					}else
					if(dInfo.getStatus() == DeleteInfo.Status.FILE_NOT_FOUND)
					{
						System.out.println("File not found in the Cumulus system");
					}
					/*FOr testing purposes store the latest log/fib in the curr dir.
					if(logFIBPair.log != null)
						logFIBPair.log.store(new File("."));
					if(logFIBPair.fib != null)
					{
						PrintWriter wr = new PrintWriter(new FileOutputStream(".cumulus.fib"));
						logFIBPair.fib.toXML(wr);
						wr.close();
					}*/
				}
				else
					System.out.println("Usage: delete <filename> [<workingfoldername>]");				
			}
		}
	}
}
