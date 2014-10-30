package edu.amrita.selabs.cumulus.lib.test;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.ActionLog;
import edu.amrita.selabs.cumulus.lib.GetLog;

public class TestGetLog {

	@Test
	public void test() throws Exception {
		GetLog log = new GetLog();
		GetLog.GetLogEntry e = new GetLog.GetLogEntry();
		e.getBadNodes().add("abcd");
		e.getBadNodes().add("deef");		
		log.getEntries().put("1234",e);
		e = new GetLog.GetLogEntry();
		e.getBadNodes().add("3456");
		e.getBadNodes().add("7890");		
		log.getEntries().put("2345",e);
		log.getProblemNodes().put("1234",  new ActionLog.ProblemNodeEntry("connect", System.currentTimeMillis()));
		log.getFibEntry().getBadNodes().add("fibbad");
		
		System.out.println(log.toXML());

	}

}
