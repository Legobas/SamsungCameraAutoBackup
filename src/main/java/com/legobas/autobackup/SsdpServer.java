package com.legobas.autobackup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsdpServer {

	private final static Logger logger = LoggerFactory.getLogger(SsdpServer.class);

	private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);

	// 34defcf8-cb1b-47fc-8e3e-6290b3e5abb0
	private String uuid;
	
	// uuid:34defcf8-cb1b-47fc-8e3e-6290b3e5abb0
	private String usn;

	// http://192.168.1.10:52235
	private String address;
	
	// http://192.168.1.10:52235/DMS/SamsungDmsDesc.xml
	private String location;

	private InetAddress upnpGroup;

	private static boolean offline = false;
	private MSearchThread listenerThread;

	public SsdpServer() {
		this.uuid = Util.generateUUID();
		this.usn = "uuid:" + this.uuid;

		try {
			InetAddress ip = InetAddress.getLocalHost();
			String ipAddress = ip.getHostAddress().trim();

			logger.info("IP:" + ipAddress);

			this.address = "http://" + ipAddress + ":52235";
			logger.debug("address: " + address);
			
			this.location = this.address + "/DMS/SamsungDmsDesc.xml";
			logger.info("Location:" + location);
		} catch (UnknownHostException e) {
			logger.error("Can't get IP address", e);
		}
	}

	private MulticastSocket getNewMulticastSocket() throws IOException {
		return getNewMulticastSocket(0);
	}

	private MulticastSocket getNewMulticastSocket(final int port) throws IOException {
		MulticastSocket ssdpSocket;
		if (port == 0) {
			ssdpSocket = new MulticastSocket();
		} else {
			ssdpSocket = new MulticastSocket(port);
		}
		ssdpSocket.setReuseAddress(true);
		// ssdpSocket.setInterface(address);
//		logger.trace("Sending message from multicast socket on network interface: "
//				+ ssdpSocket.getNetworkInterface());
//		logger.trace("Multicast socket is on interface: "
//				+ ssdpSocket.getInterface());
		ssdpSocket.setTimeToLive(32);
		// ssdpSocket.setLoopbackMode(true);
		this.upnpGroup = InetAddress.getByAddress(Constants.SSDP_HOST, Constants.UPNP_HOST_ADDRESS);
		ssdpSocket.joinGroup(this.upnpGroup);
		logger.trace("Socket Timeout: " + ssdpSocket.getSoTimeout());
		logger.trace("Socket TTL: " + ssdpSocket.getTimeToLive());

		return ssdpSocket;
	}

	private MulticastSocket getNewMulticastListenSocket() throws IOException {
		return getNewMulticastSocket(Constants.SSDP_PORT);
	}

	/**
	 * HTTP/1.1 200 OK CACHE-CONTROL:max-age=1200 DATE:Sat, 23 Jul 2011 22:13:42
	 * GMT LOCATION:http://192.168.101.227:9090/MediaserverWeb/dlna/description/
	 * fetch SERVER:Mediaserver ST:urn:schemas-upnp-org:device:MediaServer:1
	 * EXT: USN:uuid:614146ca-f169-3d3c-b228-87a712faf143::urn:schemas-upnp-org:
	 * device:MediaServer:1 Content-Length:0
	 **/

	/**
	 * HTTP/1.1 200 OK ST:urn:schemas-upnp-org:device:MediaServer:1
	 * USN:uuid:cdca376d
	 * -cb81-4b2e-95a7-9bf7dd2347a3::urn:schemas-upnp-org:device:MediaServer:1
	 * Location :http://192.168.101.227:2869/upnphost/udhisapi.dll?content=uuid:
	 * cdca376d -cb81-4b2e-95a7-9bf7dd2347a3
	 * OPT:"http://schemas.upnp.org/upnp/1/0/"; ns=01
	 * 01-NLS:b6d7028a8b1e541d7e757d0b6db33a00 Cache-Control:max-age=900
	 * Server:Microsoft-Windows-NT/5.1 UPnP/1.0 UPnP-Device-Host/1.0 Ext:
	 **/

	private void sendReply(final InetAddress remoteAddress, final int port, final String msg) throws IOException {
		DatagramSocket socket = null;
		try {
			logger.trace("create reply-socket");
			socket = new DatagramSocket();

			final DatagramPacket dgmPacket = new DatagramPacket(msg.getBytes(), msg.length(), remoteAddress, port);

			socket.send(dgmPacket);

			logger.debug("replied [" + remoteAddress + ":" + port + "] ->\n" + msg);
		} finally {
			if (socket != null) {
				logger.trace("close reply-socket");
				socket.close();
			}
		}
	}

	private String buildDiscoverMsg(String address, final String st) {

		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		final StringBuilder sb = new StringBuilder();
		String usn = this.usn;
		if (st.equals(usn)) {
			usn = "";
		} else {
			usn = "uuid:" + usn + "::";
		}

		sb.append("HTTP/1.1 200 OK");
		sb.append(Constants.CRLF);
		sb.append("ST:");
		if ((st != null) && (st.length() > 0)) {
			sb.append(st);
		}
		sb.append(Constants.CRLF);
		sb.append("USN:");
		sb.append(usn);
		sb.append(st);
		sb.append(Constants.CRLF);
		sb.append("Location:" + this.location);
		sb.append(Constants.CRLF);
		sb.append("Cache-Control:max-age=1200");
		sb.append(Constants.CRLF);
		sb.append("Server:" + Constants.SERVERNAME);
		sb.append(Constants.CRLF);
		sb.append("Date:" + sdf.format(new Date(System.currentTimeMillis())) + " GMT");
		sb.append(Constants.CRLF);
		sb.append("EXT:");
		sb.append(Constants.CRLF);

		sb.append("Content-Length:0");
		sb.append(Constants.CRLF);
		sb.append(Constants.CRLF);

		return sb.toString();
	}

	private class MSearchThread extends Thread {

		private MulticastSocket ssdpListenerSocket;

		private MSearchThread() {
			try {
				ssdpListenerSocket = new MulticastSocket(Constants.SSDP_PORT);
				InetAddress ssdpAddress = InetAddress.getByName(Constants.SSDP_HOST);
				ssdpListenerSocket.joinGroup(ssdpAddress);
			} catch (IOException e) {
				logger.error("Can't create MulticastListenSocket", e);
			}
		}

		@Override
		public void run() {
			logger.debug("M-Search Listener Started");
			setName("UPnP M-Search");
			super.run();
			while (!SsdpServer.isOffline()) {
				try {

					final DatagramPacket data = new DatagramPacket(new byte[2048], 2048);

					this.ssdpListenerSocket.receive(data);
					final String text = new String(data.getData());
					boolean answered = true;
					if (text.contains("M-SEARCH")) {
						answered = false;
						for (final String schema : new String[] { Constants.UPNP_CONTENT_DIRECTORY, Constants.UPNP_MEDIA_SERVER, Constants.UPNP_ROOTDEVICE,
								SsdpServer.this.usn }) {
							if (text.contains(schema)) {
								logger.debug("Reply to: " + schema);
								sendReply(data.getAddress(), data.getPort(), buildDiscoverMsg(data.getAddress().getHostAddress(), schema));
								answered = true;
							}
						}
						if (!answered) {
							logger.trace("ignored m-search : " + text);
						}
					}

				} catch (final Throwable e) {
					if (this.ssdpListenerSocket != null) {
						this.ssdpListenerSocket.close();
						this.ssdpListenerSocket = null;
					}
					logger.error("error while reading from upnp-network", e);
				}
			}

			logger.debug("Listener Stop");
			if (this.ssdpListenerSocket != null) {
				this.ssdpListenerSocket.close();
				this.ssdpListenerSocket = null;
			}

		}

	}

	public void startListening() {
		if (this.listenerThread == null) {
			this.listenerThread = new MSearchThread();
			offline = false;
			this.listenerThread.start();
		}
	}

	public void stopListening() {
		offline = true;
		if (this.listenerThread != null) {
			this.listenerThread.interrupt();
		}
		this.listenerThread = null;
	}

	public static boolean isOffline() {
		return offline;
	}
}
