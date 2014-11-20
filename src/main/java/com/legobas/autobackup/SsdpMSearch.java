package com.legobas.autobackup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsdpMSearch extends Thread {
	private final static Logger logger = LoggerFactory
			.getLogger(SsdpMSearch.class);

	private static SsdpMSearch thread = null;

	private MulticastSocket listener;

	private boolean receive = false;

	private List<NetworkInterface> joinedInterfaces;

	public SsdpMSearch(String name) {
		super(name);

		this.joinedInterfaces = new ArrayList<NetworkInterface>();

		try {
			this.listener = new MulticastSocket(Constants.SSDP_PORT);
//			this.listener.setSoTimeout(2000);

			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();

				if (!networkInterface.isLoopback()
						&& networkInterface.supportsMulticast()
						&& networkInterface.isUp()) {
					Enumeration<InetAddress> addrs = networkInterface
							.getInetAddresses();
					String networkName = networkInterface.getName();

					while (addrs.hasMoreElements()) {
						InetAddress addr = addrs.nextElement();

						logger.info(networkName + " IP Address: " + addr);
					}

					try {
						InetSocketAddress socketAddress = new InetSocketAddress(
								Constants.SSDP_HOST, Constants.SSDP_PORT);
						this.listener
								.joinGroup(socketAddress, networkInterface);
						this.joinedInterfaces.add(networkInterface);
						logger.info("Join group on " + networkName);
					} catch (IOException e) {
						logger.error("Cannot join group on " + networkName, e);
					}
				}
			}
		} catch (SocketException e) {
			logger.error("Socket Error", e);
		} catch (IOException e) {
			logger.error("Network Error", e);
		}
	}

	@Override
	public void run() {
		super.run();

		this.receive = true;

		logger.debug(String.format("SSDP MSEARCH Server listening to %s:%s",
				Constants.SSDP_HOST, Constants.SSDP_PORT));
		while (this.receive) {
			try {
				final DatagramPacket data = new DatagramPacket(new byte[2048],
						2048);

				this.listener.receive(data);
				final String text = new String(data.getData()).trim();

				if (text.contains("M-SEARCH")) {
					logger.trace("M-SEARCH : " + text);

					InetAddress cameraIpAddress = data.getAddress();
					int cameraPort = data.getPort();

					logger.debug("Camera IP Address: "
							+ cameraIpAddress.getHostAddress());

					String[] schemas = new String[] {
							Constants.UPNP_ROOTDEVICE,
							Constants.UPNP_MEDIA_SERVER,
							Constants.UPNP_CONTENT_DIRECTORY,
							Constants.UPNP_CONNECTION_MANAGER };
					// SsdpServer.this.usn }

					for (final String schema : schemas) {
						if (text.contains(schema)) {
							DatagramSocket socket = null;
							try {
								socket = new DatagramSocket();
								String msg = buildMessage(schema);
								final DatagramPacket response = new DatagramPacket(
										msg.getBytes(), msg.length(),
										cameraIpAddress, cameraPort);
								socket.send(response);

								// MulticastSocket ssdpSocket = null;
								// InetAddress upnpGroup =
								// InetAddress.getByAddress(Constants.SSDP_HOST,
								// Constants.UPNP_HOST_ADDRESS);
								//
								// ssdpSocket = new MulticastSocket();
								// ssdpSocket.setReuseAddress(true);
								// ssdpSocket.setTimeToLive(32);
								// ssdpSocket.joinGroup(upnpGroup);
								// logger.trace("Socket Created - Timeout:{} TTL:{}",
								// ssdpSocket.getSoTimeout(),
								// ssdpSocket.getTimeToLive());
								//
								// String msg = buildMessage(schema);
								// final DatagramPacket ssdpPacket = new
								// DatagramPacket(msg.getBytes(), msg.length(),
								// upnpGroup, Constants.SSDP_PORT);
								// ssdpSocket.send(ssdpPacket);
								//
								// logger.trace("Send message [" + upnpGroup +
								// ":" + Constants.SSDP_PORT + "] -> " + msg);
							} catch (final IOException e) {
								logger.error("Could not send M-SEARCH Reply", e);
							} finally {
								if (socket != null) {
									socket.close();
									logger.trace("SSDP M_SEARCH Response Socket Closed");
								}
							}
							break;
						}
					}

					// if (!answered) {
					// logger.trace("ignored m-search : " + text);
					// }
				}
			} catch (SocketTimeoutException s) {
				logger.debug("waiting...");
				if (!this.receive) {
					this.listener.close();
					this.listener = null;
				}
			} catch (final Throwable e) {
				if (this.receive) {
					logger.error("Error reading from upnp-network", e);
				} else {
					logger.debug("closing down...");
					this.listener.close();
				}
			}
		}
		logger.debug("SSDP MSEARCH Server stopped");
	}

	@Override
	public void interrupt() {
		super.interrupt();
		this.receive = false;
		leaveGroup();
	}

	// HTTP/1.1 200 OK
	// CACHE-CONTROL: max-age = 1800
	// EXT:
	// LOCATION: http://192.168.1.40:52235/DMS/SamsungDmsDesc.xml
	// SERVER: MS-Windows/XP UPnP/1.0 PROTOTYPE/1.0
	// ST: urn:schemas-upnp-org:device:MediaServer:1
	// USN:
	// uuid:4a682b0b-0361-dbae-6155-2eb8420974ae::urn:schemas-upnp-org:device:MediaServer:1
	// Content-Length: 0
	private String buildMessage(String nt) {
		final StringBuilder sb = new StringBuilder();

		sb.append("HTTP/1.1 200 OK");
		sb.append(Constants.CRLF);
		sb.append("CACHE-CONTROL: max-age=1800");
		sb.append(Constants.CRLF);
		sb.append("EXT: ");
		sb.append(Constants.CRLF);
		sb.append("LOCATION: " + Server.getLocation());
		sb.append(Constants.CRLF);
		sb.append("SERVER: " + Constants.SERVERNAME);
		sb.append(Constants.CRLF);
		sb.append("ST: " + nt);
		sb.append(Constants.CRLF);
		sb.append("USN: " + Server.getUsn());
		if (!nt.startsWith("uuid")) {
			sb.append("::" + nt);
		}
		sb.append(Constants.CRLF);
		sb.append("Content-Length: 0");
		sb.append(Constants.CRLF);
		sb.append(Constants.CRLF);

		return sb.toString();
	}

	private void leaveGroup() {
		logger.debug("leaveGroup()");
		if (this.listener != null) {
			try {
				InetSocketAddress socketAddress = new InetSocketAddress(
						Constants.SSDP_HOST, Constants.SSDP_PORT);
				for (NetworkInterface joinedInterface : this.joinedInterfaces) {
					logger.debug("leaving " + joinedInterface.getName());
					this.listener.leaveGroup(socketAddress, joinedInterface);
				}
			} catch (IOException ioe) {
				logger.error("Cannot leave group", ioe);
				ioe.printStackTrace();
			}
		}
		logger.debug("leaveGroup() end");
	}

	public static void startServer() {
		logger.debug("startServer()");
		if (thread == null) {
			thread = new SsdpMSearch("UPnP M-Search");
			thread.start();
		}
	}

	public static void stopServer() {
		logger.debug("stopServer()");
		if (thread != null) {
			thread.receive = false;
			thread.interrupt();
		}
	}

}
