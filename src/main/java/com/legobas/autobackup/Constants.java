package com.legobas.autobackup;

public class Constants {
	public static final String UPNP_ROOTDEVICE = "upnp:rootdevice";
	public static final String UPNP_MEDIA_SERVER = "urn:schemas-upnp-org:device:MediaServer:1";
	public static final String UPNP_CONTENT_DIRECTORY = "urn:schemas-upnp-org:service:ContentDirectory:1";
	public static final String UPNP_CONNECTION_MANAGER = "urn:schemas-upnp-org:service:ConnectionManager:1";
	public static final byte[] UPNP_HOST_ADDRESS = { (byte) 239, (byte) 255, (byte) 255, (byte) 250 };
	public static final String SSDP_HOST = "239.255.255.250";
	public static final int SSDP_PORT = 1900;
	public static final String BYEBYE = "ssdp:byebye";
	public static final String ALIVE = "ssdp:alive";
	public static final String CRLF = "\r\n";
	public static final String SERVERNAME = "MS-Windows/XP UPnP/1.0 PROTOTYPE/1.0";
	public static final String SERVERPORT = "52235";
}
