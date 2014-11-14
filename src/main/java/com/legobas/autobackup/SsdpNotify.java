package com.legobas.autobackup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsdpNotify extends TimerTask {
	private final static Logger logger = LoggerFactory.getLogger(SsdpNotify.class);

	public SsdpNotify() {
	}

	@Override
	public void run() {
		logger.info("SSPD Notify");
		try {
			this.sendAlive();
		} catch (IOException e) {
			logger.error("SSDP Send Notify error", e);
		}

		logger.trace("SSDP Notify sent: " + new Date());
	}

	// NOTIFY * HTTP/1.1
	// HOST: 239.255.255.250:1900
	// CACHE-CONTROL: max-age=1800
	// LOCATION: http://192.168.1.40:52235/DMS/SamsungDmsDesc.xml
	// NT: urn:schemas-upnp-org:service:ConnectionManager:1
	// NTS: ssdp:alive
	// USN:
	// uuid:4a682b0b-0361-dbae-6155-2eb8420974ae::urn:schemas-upnp-org:service:ConnectionManager:1
	// SERVER: MS-Windows/XP UPnP/1.0 PROTOTYPE/1.0
	// Content-Length: 0
	public void sendAlive() throws IOException {
		if (Server.getLocation() != null && !Server.getLocation().isEmpty()) {
			logger.debug("Sending ALIVE...");

			String[] devices = new String[] { Constants.UPNP_ROOTDEVICE, Server.getUsn(), Constants.UPNP_MEDIA_SERVER, Constants.UPNP_CONTENT_DIRECTORY,
					Constants.UPNP_CONNECTION_MANAGER };

			for (final String device : devices) {
				MulticastSocket ssdpSocket = null;
				try {
					InetAddress upnpGroup = InetAddress.getByAddress(Constants.SSDP_HOST, Constants.UPNP_HOST_ADDRESS);

					ssdpSocket = new MulticastSocket();
					ssdpSocket.setReuseAddress(true);
					ssdpSocket.setTimeToLive(32);
					ssdpSocket.joinGroup(upnpGroup);
					logger.trace("Socket Created - Timeout:{} TTL:{}", ssdpSocket.getSoTimeout(), ssdpSocket.getTimeToLive());

					String msg = buildMessage(device, Constants.ALIVE);
					final DatagramPacket ssdpPacket = new DatagramPacket(msg.getBytes(), msg.length(), upnpGroup, Constants.SSDP_PORT);
					ssdpSocket.send(ssdpPacket);

					logger.trace("Send message [" + upnpGroup + ":" + Constants.SSDP_PORT + "] -> " + msg);
				} catch (final IOException e) {
					logger.error("Could not send SSDP Notify " + Constants.BYEBYE, e);
				} finally {
					if (ssdpSocket != null) {
						ssdpSocket.close();
						logger.trace("SSDP Notify Socket Closed");
					}
				}
			}
		}
	}

	/**
	 * NOTIFY * HTTP/1.1 Host:[FF02::C]:1900
	 * NT:uuid:39eb073b-14a7-4d00-a3c7-91ee88978a77 NTS:ssdp:byebye
	 * Location:http
	 * ://[fe80::6503:d31b:18c7:2f69]:2869/upnphost/udhisapi.dll?content
	 * =uuid:39eb073b-14a7-4d00-a3c7-91ee88978a77
	 * USN:uuid:39eb073b-14a7-4d00-a3c7-91ee88978a77 Cache-Control:max-age=1800
	 * Server:Microsoft-Windows-NT/5.1 UPnP/1.0 UPnP-Device-Host/1.0
	 * OPT:"http://schemas.upnp.org/upnp/1/0/"; ns=01
	 * 01-NLS:bb72a37c619b42ac9f26689d1980cd7d
	 **/
	public void sendByeBye() {
		if (Server.getLocation() != null && !Server.getLocation().isEmpty()) {
			logger.debug("Sending ByeBye...");

			String[] devices = new String[] { Constants.UPNP_ROOTDEVICE, Server.getUsn(), Constants.UPNP_MEDIA_SERVER, Constants.UPNP_CONTENT_DIRECTORY,
					Constants.UPNP_CONNECTION_MANAGER };

			for (final String device : devices) {
				MulticastSocket ssdpSocket = null;
				try {
					InetAddress upnpGroup = InetAddress.getByAddress(Constants.SSDP_HOST, Constants.UPNP_HOST_ADDRESS);

					ssdpSocket = new MulticastSocket();
					ssdpSocket.setReuseAddress(true);
					ssdpSocket.setTimeToLive(32);
					ssdpSocket.joinGroup(upnpGroup);
					logger.trace("Socket Created - Timeout:{} TTL:{}", ssdpSocket.getSoTimeout(), ssdpSocket.getTimeToLive());

					String msg = buildMessage(device, Constants.BYEBYE);
					final DatagramPacket ssdpPacket = new DatagramPacket(msg.getBytes(), msg.length(), upnpGroup, Constants.SSDP_PORT);
					ssdpSocket.send(ssdpPacket);

					logger.trace("Send message [" + upnpGroup + ":" + Constants.SSDP_PORT + "] -> " + msg);
				} catch (final IOException e) {
					logger.error("Could not send SSDP Notify " + Constants.BYEBYE, e);
				} finally {
					if (ssdpSocket != null) {
						ssdpSocket.close();
						logger.trace("SSDP Notify Socket Closed");
					}
				}
			}
		}
	}

	// NOTIFY * HTTP/1.1
	// HOST: 239.255.255.250:1900
	// CACHE-CONTROL: max-age=1800
	// LOCATION: http://192.168.1.40:52235/DMS/SamsungDmsDesc.xml
	// NT: urn:schemas-upnp-org:service:ConnectionManager:1
	// NTS: ssdp:alive
	// USN:
	// uuid:4a682b0b-0361-dbae-6155-2eb8420974ae::urn:schemas-upnp-org:service:ConnectionManager:1
	// SERVER: MS-Windows/XP UPnP/1.0 PROTOTYPE/1.0
	// Content-Length: 0
	private String buildMessage(String nt, final String message) {
		final StringBuilder sb = new StringBuilder();

		sb.append("NOTIFY * HTTP/1.1");
		sb.append(Constants.CRLF);
		sb.append("HOST: " + Constants.SSDP_HOST + ":" + Constants.SSDP_PORT);
		sb.append(Constants.CRLF);
		sb.append("CACHE-CONTROL: max-age=1800");
		sb.append(Constants.CRLF);
		sb.append("LOCATION: " + Server.getLocation());
		sb.append(Constants.CRLF);
		sb.append("NT: " + nt);
		sb.append(Constants.CRLF);
		sb.append("NTS: " + message);
		sb.append(Constants.CRLF);
		sb.append("USN: " + Server.getUsn());
		if (!nt.startsWith("uuid")) {
			sb.append("::" + nt);
		}
		sb.append(Constants.CRLF);
		sb.append("SERVER: " + Constants.SERVERNAME);
		sb.append(Constants.CRLF);
		sb.append(Constants.CRLF);
		
		return sb.toString();
	}

}
