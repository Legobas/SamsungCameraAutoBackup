package com.legobas.autobackup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(HttpServer.class);

	private static HttpServer thread = null;

	private ServerSocket listener;

	public HttpServer(String name) {
		super(name);

		int port = Integer.parseInt(Constants.SERVERPORT);
		try {
			this.listener = new ServerSocket(port);
		} catch (IOException e) {
			logger.error("HTTP Server Error", e);
		}
	}

	@Override
	public void run() {
		super.run();

		logger.debug("HTTP Server listening to port " + Constants.SERVERPORT);
		while (this.listener != null) {
			Socket socket = null;
			try {
logger.debug("accept");
				socket = this.listener.accept();
logger.debug("read");
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				String textLine;
				String messageMode = "";
				String xml = "";

				boolean post = false;
				while ((textLine = reader.readLine()) != null) {
					logger.trace("Request Line: " + textLine);
					if (textLine.startsWith("GET ")) {
						// GET /DMS/SamsungDmsDesc.xml HTTP/1.1
						String document = StringUtils.substringBetween(textLine, "/DMS/", ".xml");
						if (document.equals("SamsungDmsDesc")) {
							messageMode = MessageBuilder.MODE_DMS;
						} else if (document.equals("ConnectionManager1")) {
							messageMode = MessageBuilder.MODE_CONNECTIONMANAGER;
						} else if (document.equals("ContentDirectory1")) {
							messageMode = MessageBuilder.MODE_CONTENTDIRECTORY;
						} else {
							logger.warn("Unknown document request: '{}'", document);
						}
					}
					if (textLine.startsWith("POST ")) {
						// POST /upnp/control/ContentDirectory1 HTTP/1.1
						post = true;
					}
					if (post && textLine.startsWith("SOAPACTION: ") && textLine.endsWith("#X_BACKUP_START")) {
						messageMode = MessageBuilder.MODE_BACKUP_START;
					}
					if (post && textLine.startsWith("SOAPACTION: ") && textLine.endsWith("#CreateObject")) {
						messageMode = MessageBuilder.MODE_CREATE_OBJECT;
					}
					if (post && textLine.startsWith("SOAPACTION: ") && textLine.endsWith("#X_BACKUP_DONE")) {
						messageMode = MessageBuilder.MODE_BACKUP_DONE;
					}
					if (post && textLine.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
						xml = textLine;
					}
				}

				logger.debug("Message Mode: '{}'", messageMode);
				if (StringUtils.isNotEmpty(messageMode)) {

					String objectId = "";
					if (StringUtils.isNotEmpty(xml)) {
						Didl didl = MessageParser.getDidl(xml);
						logger.debug("Response DIDL: \n{}", didl);
					}

					// Write Response
					PrintWriter response = new PrintWriter(socket.getOutputStream(), true);

					// send an HTTP response to the client
					response.print(MessageBuilder.buildMessage(messageMode, objectId));
					response.flush();
					response.close();

					logger.trace("Response Sent");
				} else {
					logger.trace("No Response Sent");
				}
			} catch (IOException e) {
				logger.error("HTTP Request/Response Error", e);
			} finally {
				logger.debug("finally");
				if (socket != null) {
					try {
						socket.close();
						logger.trace("HTTP Socket Closed");
					} catch (IOException e) {
						logger.error("Could not close HTTP Socket", e);
					}
					socket = null;
				}
			}
		}
		logger.debug("HTTP Server stopped");
	}

	public static void startServer() {
		logger.debug("startServer()");
		if (thread == null) {
			thread = new HttpServer("HTTP Server");
			thread.start();
		}
	}

	public static void stopServer() {
		logger.debug("stopServer()");
		if (thread != null) {
			try {
				thread.listener.close();
				logger.debug("listener closed");
			} catch (IOException e) {
				logger.error("Could not close HTTP Socket", e);
			}
//			thread.interrupt();
//			thread = null;
		}
	}

}
