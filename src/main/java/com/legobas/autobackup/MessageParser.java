package com.legobas.autobackup;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MessageParser {
	private final static Logger logger = LoggerFactory.getLogger(MessageParser.class);

	public static Didl getDidl(String xml) {
		String didlXml = StringUtils.substringBetween(xml, "<Elements>", "</Elements>");

		didlXml = StringEscapeUtils.unescapeXml(didlXml);

		return parseDidl(didlXml);
	}

//  Request Example:
//	<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/" xmlns:dlna = "urn:schemas-dlna-org:metadata-1-0/">
//	  <item id="" restricted="0" parentID="DLNA.ORG_AnyContainer" >
//	    <dc:title>SAM_0308.JPG</dc:title>
//	    <dc:date>2014-05-24</dc:date>
//	    <upnp:class>object.item.imageItem</upnp:class>
//	    <res protocolInfo="*:*:image/jpeg:DLNA.ORG_PN=JPEG_LRG;DLNA.ORG_CI=0" size="5231198" />
//	  </item>
//	</DIDL-Lite>

	protected static Didl parseDidl(String xml) {
		String title = StringUtils.substringBetween(xml, "<dc:title>", "</dc:title>");
		String date = StringUtils.substringBetween(xml, "<dc:date>", "</dc:date>");
		String size = StringUtils.substringBetween(xml, " size=\"", "\"");

		return new Didl(title, date, Long.parseLong(size));
	}

	public static String prepareXml(InputStream is) {
		String result = null;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(is);

			Node device = doc.getElementsByTagName("device").item(0);
			NodeList list = device.getChildNodes();

			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);

				if (node.getNodeName().equals("friendlyName")) {
					node.setTextContent("TestFriendlyName");
				}
				if (node.getNodeName().equals("UDN")) {
					node.setTextContent("TestUDN");
				}
			}

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			result = writer.getBuffer().toString();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
}
