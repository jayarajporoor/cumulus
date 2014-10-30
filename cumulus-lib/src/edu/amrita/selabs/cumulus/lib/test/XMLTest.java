package edu.amrita.selabs.cumulus.lib.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;

public class XMLTest {

	@Test
	public void test() throws Exception {
		String xml = "<?xml version=\"1.0\" ?>\n<books>\n<book rare=\"yes\">Test 1 </book>\n<book>Test 2</book>\n</books>";
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
        Document doc = builder.parse(is);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString(); 
        System.out.println(output);
	}
	
}
