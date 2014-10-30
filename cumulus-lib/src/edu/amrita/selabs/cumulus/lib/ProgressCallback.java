package edu.amrita.selabs.cumulus.lib;

public interface ProgressCallback {
	public void taskBegin(String name);
	public void taskEnd();
	public void taskPartEnd();
	public void taskError(String desc);
	public void taskProgress(String desc);
}
