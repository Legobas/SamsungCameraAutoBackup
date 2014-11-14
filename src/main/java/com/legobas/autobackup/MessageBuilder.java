package com.legobas.autobackup;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class MessageBuilder.
 *
 * @author Bas de Roos
 */
public class MessageBuilder {

	/** The Constant logger. */
	private final static Logger logger = LoggerFactory.getLogger(MessageBuilder.class);

	/** The Constant MODE_DMS. */
	public final static String MODE_DMS = "MODE_DMS";

	/** The Constant MODE_CONNECTIONMANAGER. */
	public final static String MODE_CONNECTIONMANAGER = "MODE_CONNECTIONMANAGER";

	/** The Constant MODE_CONTENTDIRECTORY. */
	public final static String MODE_CONTENTDIRECTORY = "MODE_CONTENTDIRECTORY";

	/** The Constant MODE_BACKUP_START. */
	public final static String MODE_BACKUP_START = "MODE_BACKUP_START";

	/** The Constant MODE_CREATE_OBJECT. */
	public final static String MODE_CREATE_OBJECT = "MODE_CREATE_OBJECT";

	/** The Constant MODE_BACKUP_DONE. */
	public final static String MODE_BACKUP_DONE = "MODE_BACKUP_DONE";

	/** The Constant XML_PATH_DMS. */
	private final static String XML_PATH_DMS = "/DMS/SamsungDmsDesc.xml";

	/** The Constant XML_PATH_CONNECTIONMANAGER. */
	private final static String XML_PATH_CONNECTIONMANAGER = "/DMS/ConnectionManager1.xml";

	/** The Constant XML_PATH_CONTENTDIRECTORY. */
	private final static String XML_PATH_CONTENTDIRECTORY = "/DMS/ContentDirectory1.xml";

	/** The Constant XML_PATH_BACKUP_START. */
	private final static String XML_PATH_BACKUP_START = "/upnp/control/ContentDirectory1-X_BACKUP_START.xml";

	/** The Constant XML_PATH_CREATE_OBJECT. */
	private final static String XML_PATH_CREATE_OBJECT = "/upnp/control/ContentDirectory1-CreateObject.xml";

	/** The Constant XML_PATH_BACKUP_DONE. */
	private final static String XML_PATH_BACKUP_DONE = "/upnp/control/ContentDirectory1-X_BACKUP_DONE.xml";

	/**
	 * Builds the message.
	 *
	 * @param mode the mode
	 * @param objectId the object id
	 * @return the string
	 */
	public static String buildMessage(String mode, String objectId) {
		logger.debug("buildMessage({})", mode);

		String xmlPath = "";

		if (mode.equals(MODE_DMS)) {
			xmlPath = XML_PATH_DMS;
		} else if (mode.equals(MODE_CONNECTIONMANAGER)) {
			xmlPath = XML_PATH_CONNECTIONMANAGER;
		} else if (mode.equals(MODE_CONTENTDIRECTORY)) {
			xmlPath = XML_PATH_CONTENTDIRECTORY;
		} else if (mode.equals(MODE_BACKUP_START)) {
			xmlPath = XML_PATH_BACKUP_START;
		} else if (mode.equals(MODE_CREATE_OBJECT)) {
			xmlPath = XML_PATH_CREATE_OBJECT;
		} else if (mode.equals(MODE_BACKUP_DONE)) {
			xmlPath = XML_PATH_BACKUP_DONE;
		} else {
			logger.error("Unknown Mode: '{}'", mode);
		}

		String xml = "";
		try {
			xml = IOUtils.toString(MessageBuilder.class.getResourceAsStream(xmlPath), "UTF-8");
		} catch (IOException e) {
			logger.error("Cannot read {}", xmlPath);
		}

//		String xml = FileUtils.readFileToString(FileUtils.toFile(MessageBuilder.class.getResourceAsStream(xmlPath)));

//		String xml = "";
//		try {
//			InputStream is = MessageBuilder.class.getResourceAsStream(xmlPath);
//			InputStreamReader isr = new InputStreamReader(is);
//			BufferedReader br = new BufferedReader(isr);
//			String line;
//			while ((line = br.readLine()) != null) {
//				xml += line;
//				xml += "\n";
//			}
//			br.close();
//			isr.close();
//			is.close();
//		} catch (IOException e) {
//			logger.error("Cannot get " + xmlPath, e);
//		} catch (NullPointerException e) {
//			logger.error("Cannot get " + xmlPath, e);
//		}

		// Fill variables in XML
		if (xml.contains("%(friendly_name)s")) {
			xml = StringUtils.replace(xml, "%(friendly_name)s", Server.getFriendlyName());
		}
		if (xml.contains("uuid:%(uuid)s")) {
			xml = StringUtils.replace(xml, "uuid:%(uuid)s", Server.getUsn());
		}

		if (xml.contains("%(object_id)s")) {
			xml = StringUtils.replace(xml, "%(object_id)s", objectId);
		}
		if (xml.contains("%(didl_lite)s")) {
			String didl = buildDidl("objId", "parentId", "objectType", "objectSubtype", "objectClass");
			didl = StringEscapeUtils.escapeXml10(didl);
			xml = xml.replace("%(didl_lite)s", didl);
		}

		final StringBuilder sb = new StringBuilder();

		sb.append("HTTP/1.1 200 OK");
		sb.append(Constants.CRLF);
		sb.append("Content-Length: " + String.valueOf(xml.length()));
		sb.append(Constants.CRLF);
		sb.append("Content-Type: text/xml; charset=\"UTF-8\"");
		sb.append(Constants.CRLF);
		sb.append("Date: " + getHttpDate()); // Wed, 28 May 2014 10:26:40 GMT
		sb.append(Constants.CRLF);
		sb.append("Server: DMRND/0.5");
		sb.append(Constants.CRLF);
		sb.append(Constants.CRLF);

		sb.append(xml);

		logger.trace("buildMessage({}) returns:\n{}\n", mode, sb.toString());
		return sb.toString();
	}

	/**
	 * Builds the didl.
	 *
	 * @param objId the obj id
	 * @param parentId the parent id
	 * @param objectType the object type
	 * @param objectSubtype the object subtype
	 * @param objectClass the object class
	 * @return the string
	 */
	protected static String buildDidl(String objId, String parentId, String objectType, String objectSubtype, String objectClass) {
		final StringBuilder sb = new StringBuilder();

		sb.append("<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp='urn:schemas-upnp-org:metadata-1-0/upnp/' xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\" xmlns:sec=\"http://www.sec.co.kr/\">");
		sb.append("<item id=\"" + objId + "\" parentID=\"" + parentId + "\" restricted=\"0\" dlna:dlnaManaged=\"00000004\">");
		sb.append("<dc:title></dc:title>");
		sb.append("<res protocolInfo=\"http-get:*:" + objectType + ":" + objectSubtype + ";DLNA.ORG_CI=0;DLNA.ORG_FLAGS=00D00000000000000000000000000000\"");
		sb.append(" importUri=\"" + Server.getAddress() + "/cd/content?didx=0_id=%(obj_id)s\"");
		sb.append(" dlna:resumeUpload=\"0\" dlna:uploadedSize=\"0\" size=\"%(obj_size)s\">");
		sb.append("</res>");
		sb.append("<upnp:class>" + objectClass + "</upnp:class>");
		sb.append("</item>");
		sb.append("</DIDL-Lite>");

		logger.trace("buildDidl() returns:\n{}\n", sb.toString());
		return sb.toString();
	}

	/**
	 * Gets the http date.
	 *
	 * @return the http date
	 */
	private static String getHttpDate() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}

}
