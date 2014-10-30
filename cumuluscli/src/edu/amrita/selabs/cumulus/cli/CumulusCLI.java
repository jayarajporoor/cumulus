package edu.amrita.selabs.cumulus.cli;

import java.io.File;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Properties;

import edu.amrita.selabs.cumulus.lib.DefaultCumulusClient;
import edu.amrita.selabs.cumulus.lib.DeleteInfo;
import edu.amrita.selabs.cumulus.lib.GetInfo;
import edu.amrita.selabs.cumulus.lib.ProgressCallback;
import edu.amrita.selabs.cumulus.lib.PutLog;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

public class CumulusCLI{
	public static void main(String[] args) throws Exception
	{
		ConsoleReader reader = new ConsoleReader();
		reader.setPrompt("cumulus:>");
		reader.addCompleter(new FileNameCompleter());
		final PrintWriter out = new PrintWriter(reader.getOutput());
		
		Properties props = new Properties();;
		if(args.length > 0)
		{
			FileInputStream in = new FileInputStream(args[0]);
			props.load(in);
			in.close();
		}else
		{
			out.println("Give properties file as argument");
			return;
		}
		
		DefaultCumulusClient dcc = new DefaultCumulusClient();
		dcc.setProgressCallback(
			new ProgressCallback()
			{
				public void taskBegin(String name)
				{
					out.print("Starting " + name);
					out.flush();
				}
				
				public void taskEnd()
				{
					out.println(" Completed.");
					out.flush();					
				}
				
				public void taskError(String desc)
				{
					out.println(" Task completed with errors: " + desc + " See log for more details.");
					out.flush();					
				}
				
				public void taskProgress(String desc)
				{
					out.print(".");
					out.flush();					
				}
				
				public void taskPartEnd()
				{
					out.println(" Partially completed.");
					out.flush();					
				}

			}
		);		
		dcc.setProperties(props);
		
		int replicationFactor = Integer.parseInt(props.getProperty("cumulus.replication.factor", "1"));
		int blockSize = Integer.parseInt(props.getProperty("cumulus.blocksize", "" + dcc.minBlockSize));
		
		String line;
		while((line = reader.readLine())!= null)
		{
			String[] fields = line.split(" ");
			if("exit".equals(fields[0]))
			{
				out.println("Bye!");
				return;
			}else
			if("put".equals(fields[0]))
			{
				File file = new File(fields[1]);
				PutLog log = dcc.put(fields[1]);
				if(!log.fibDone(replicationFactor) || !log.allBlocksDone(replicationFactor, blockSize, file.length()))
				{
					out.println("put operation has only partially completed. Please rerun after some time to complete.");
				}
			}else
			if("get".equals(fields[0]))
			{
				if(fields.length < 2)
				{
					out.println("Usage: get <filename> [<targetfoldername>]");
				}else
				{
					String folder = ".";
					if(fields.length >= 3)
						folder = fields[2];
				
					GetInfo gInfo = dcc.get(fields[1], folder);
					if(gInfo.getStatus() == GetInfo.Status.IN_PROGRESS)
					{
						out.println("get operation has only partially completed. Please rerun after some time to complete.");
					}else
					if(gInfo.getStatus() == GetInfo.Status.FILE_NOT_FOUND)
					{
						out.println("File not found in the Cumulus system");
					}else
					if(gInfo.getStatus() == GetInfo.Status.COMBINE_FAILED)
					{
						out.println("Combine phase failed to build the target file. See log for details.");
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
						out.println("delete operation has only partially completed. Please rerun after some time to complete.");
					}else
					if(dInfo.getStatus() == DeleteInfo.Status.FILE_NOT_FOUND)
					{
						out.println("File not found in the Cumulus system");
					}
				}
				else
					out.println("Usage: delete <filename> [<workingfoldername>]");				
			}
		}
	}
	
}
