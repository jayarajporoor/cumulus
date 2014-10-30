package edu.amrita.selabs.cumulus.lib;

import edu.amrita.selabs.cumulus.lib.GetLog.GetLogEntry;

public abstract class GetOrDeleteLog extends ActionLog {
	GetLog.GetLogEntry fibEntry;
	
	public GetOrDeleteLog()
	{
		super();
		fibEntry = new GetLog.GetLogEntry();
	}
	
	public GetLogEntry getFibEntry()
	{
		return fibEntry;
	}
	
	public void setFibEntry(GetLogEntry e)
	{
		fibEntry = e;
	}
	
}
