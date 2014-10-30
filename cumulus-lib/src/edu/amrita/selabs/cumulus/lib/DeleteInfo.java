package edu.amrita.selabs.cumulus.lib;

public class DeleteInfo {
	public enum Status {FILE_NOT_FOUND, IN_PROGRESS, DONE};
	DeleteLog log=null;
	FileInfoBlock fib =null;
	
	Status status = Status.IN_PROGRESS;
	
	public Status getStatus()
	{
		return status;
	}
	
	public void setStatus(Status st)
	{
		this.status = st;
	}
	
	public DeleteLog getLog() {
		return log;
	}
	public void setLog(DeleteLog log) {
		this.log = log;
	}
	public FileInfoBlock getFib() {
		return fib;
	}
	public void setFib(FileInfoBlock fib) {
		this.fib = fib;
	}

	
}
