package com.legobas.autobackup;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceWrapper implements Daemon {
	private final static Logger logger = LoggerFactory
			.getLogger(ServiceWrapper.class);

	private static final ServiceWrapper serviceWrapper = new ServiceWrapper();

	public static void main(String[] args) throws Exception {
		if (args != null && args.length > 0) {
			String mode = args[0];
			logger.info("ServiceWrapper main started");

			if ("start".equals(mode)) {
				logger.debug("ServiceWrapper starting service");
				serviceWrapper.init(null);
				serviceWrapper.start();
			} else {
				logger.debug("ServiceWrapper stopping service");
				serviceWrapper.stop();
				serviceWrapper.destroy();
			}
		}
	}

	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException,
			Exception {
		logger.info("ServiceWrapper init called");
	}

	@Override
	public void start() throws Exception {
		logger.info("ServiceWrapper start called");
		Server.class.getDeclaredMethod("main", String[].class).invoke(null,
				(Object) null);
	}

	@Override
	public void stop() throws Exception {
		logger.info("ServiceWrapper halt called");
		Server.class.getDeclaredMethod("halt").invoke(null);
		
		// Service must listen to socket for stop command
//		ServerSocket server = new ServerSocket(8080);
//		System.out.println("Socket listening!");
//		server.accept();
//		System.out.println("Connection received!");		
	}

	@Override
	public void destroy() {
		logger.info("ServiceWrapper destroy called");
	}
}
