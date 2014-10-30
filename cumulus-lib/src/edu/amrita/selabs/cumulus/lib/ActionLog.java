package edu.amrita.selabs.cumulus.lib;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ActionLog {
	HashMap<String, ProblemNodeEntry> problemNodes;
	
	public static final long DEFAULT_REFRESH_INTERVAL = 60*5;
	Logger logger;
	
	
	public ActionLog()
	{
		problemNodes = new HashMap<String, ProblemNodeEntry>();
		logger = LoggerFactory.getLogger(this.getClass());		
	}
	
	public HashMap<String, ProblemNodeEntry> getProblemNodes() {
		return problemNodes;
	}

	public void setProblemNodes(HashMap<String, ProblemNodeEntry> problemNodes) {
		this.problemNodes = problemNodes;
	}
	
	public void refreshProblemNodes(long removalPeriodInSec)
	{
		Iterator<String> it = problemNodes.keySet().iterator();
		long currTime = System.currentTimeMillis();
		long removalPeriodInMillis = removalPeriodInSec * 1000;
		while(it.hasNext())
		{
			String nodeid = it.next();
			ProblemNodeEntry e = problemNodes.get(nodeid);
			if(currTime - e.timestamp > removalPeriodInMillis)
			{
				if(logger.isDebugEnabled())
					logger.debug("Removing problem node: {}", nodeid);
				it.remove();
			}
		}
	}
	
	public abstract void store(File workingFolder) throws Exception;
	
	public static class ProblemNodeEntry
	{
		String desc;
		long   timestamp;
		
		public ProblemNodeEntry()
		{
			
		}
		
		public ProblemNodeEntry(String desc, long timestamp)
		{
			this.desc = desc;
			this.timestamp = timestamp;
		}
		
		public String getDesc() {
			return desc;
		}
		
		public void setDesc(String desc) {
			this.desc = desc;
		}
				
		public long getTimestamp() {
			return timestamp;
		}
		
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
		
		
	}
}
