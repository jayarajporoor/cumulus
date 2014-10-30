package edu.amrita.selabs.cumulus.lib.test;

import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


import org.junit.Test;

public class LoggerTest {

	@Test
	public void test() {
	   Logger logger = LoggerFactory.getLogger(this.getClass());
	   MDC.put("first",  "Testing");
	    logger.info("Hello World {} {}", "Test", new Integer(1));
		   MDC.put("first",  "Working");
	    logger.debug("Aum {} {}", "Amma", new Integer(1));
	    //logger.error("Error", new Exception("Ok"));
	    logger.error("Test {} {}", "Case", new Exception("Ok"));

	}

}
