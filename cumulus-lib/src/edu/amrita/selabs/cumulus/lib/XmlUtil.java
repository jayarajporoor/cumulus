package edu.amrita.selabs.cumulus.lib;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlUtil {

	public static String getChildAsString(Element e, String child)
	{
		NodeList nodes = e.getElementsByTagName(child);
		if(nodes.getLength() > 0)
		{
			Element i = (Element) nodes.item(0);
			return i.getTextContent().trim();
		}else
			return null;
	}
	
	public static void printXml(Document doc, Writer writer) throws Exception
	{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    transformer.transform(new DOMSource(doc), new StreamResult(writer));
	}

}
