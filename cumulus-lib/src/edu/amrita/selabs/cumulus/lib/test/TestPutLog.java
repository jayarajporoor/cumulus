package edu.amrita.selabs.cumulus.lib.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Test;

import edu.amrita.selabs.cumulus.lib.PutLog;

public class TestPutLog {

	@Test
	public void test() throws Exception{
		PutLog log = new PutLog();
		System.out.println(log.toXML());
		HashSet<String> n = new HashSet<String>();
		n.add("3456");
		n.add("7890");
		log.putEntry(1, "1234", "123456", n, 100);
		n = new HashSet<String>();
		n.add("ABCD");
		n.add("CDEF");
		log.putEntry(2, "5678", "567890", n, 100);
		PutLog.PutLogEntry entry = new PutLog.PutLogEntry();
		entry.setBid("1234");
		entry.setPlainHash("3456");
		entry.getToNotifyServer().add("1");
		entry.getToNotifyServer().add("2");		
		log.putEntry(3, entry);
		log.store(new File("/tmp"));

		System.out.println(log.toXML());
	}
	
	@Test
	public void testLoad() throws Exception {
		PutLog log = PutLog.loadIfExists(new File("/tmp"));
		System.out.println(log.toXML());
	}

}
