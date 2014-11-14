package com.legobas.autobackup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

	private final static Logger logger = LoggerFactory.getLogger(Server.class);

	private static Timer timer = null;

	private static String uuid;
	private static String usn;
	private static String address;
	private static String location;
	private static InetAddress ipAddress;
	private static String friendlyName = "[JAVA]AutoBackup";

	public static SsdpServer server = new SsdpServer();

	public static void main(String[] args) {
		logger.info("Server main()");

		uuid = Util.generateUUID();
		usn = "uuid:" + uuid;

		logger.debug("uuid: " + uuid);
		logger.debug("usn: " + usn);

		try {
			ipAddress = Util.getLocalHostLANAddress();
		} catch (UnknownHostException e) {
			logger.error("No IP address found", e);
		}

		address = "http://" + ipAddress.getHostAddress() + ":"
				+ Constants.SERVERPORT;
		logger.debug("address: " + address);

		location = address + "/DMS/SamsungDmsDesc.xml";
		logger.debug("location: " + location);

		// timer = new Timer();
		// timer.schedule(new SsdpNotify(), 0, 30 * 1000); // 5 Min.

		SsdpMSearch.startServer();
		HttpServer.startServer();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Server.shutdown();
			}
		});
	}

	public static void shutdown() {
		logger.info("Server shutdown()");

		if (timer != null) {
			timer.cancel();
		}

		SsdpMSearch.stopServer();
		HttpServer.stopServer();
	}

	public static String getUsn() {
		return usn;
	}

	public static String getAddress() {
		return address;
	}

	public static String getLocation() {
		return location;
	}

	public static String getFriendlyName() {
		return friendlyName;
	}

}
