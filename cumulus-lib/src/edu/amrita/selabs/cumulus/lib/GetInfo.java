package edu.amrita.selabs.cumulus.lib;

public class GetInfo {
	public enum Status {FILE_NOT_FOUND, IN_PROGRESS, DONE, COMBINE_FAILED};
	FileInfoBlock fib = null;
	GetLog 		  log  = null;
	Status 		  status = Status.IN_PROGRESS;
	
	public void setStatus(Status st)
	{
		status = st;
	}
	
	public Status getStatus()
	{
		return status;
	}
	
	public FileInfoBlock getFib() {
		return fib;
	}
	public void setFib(FileInfoBlock fib) {
		this.fib = fib;
	}
	public GetLog getLog() {
		return log;
	}
	public void setLog(GetLog log) {
		this.log = log;
	}

	
}
