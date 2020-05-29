package org.py4j.defaultserver;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.py4j.defaultserver.preferences.PreferenceConstants;

import py4j.GatewayServer;

public class DefaultServerActivator extends AbstractUIPlugin {

	private static BundleContext context;

	private static DefaultServerActivator activator;

	private GatewayServer server;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		DefaultServerActivator.context = bundleContext;
		activator = this;
		IPreferenceStore store = getPreferenceStore();

		final boolean enabled = store.getBoolean(PreferenceConstants.PREF_PY4J_ACTIVE);
		boolean override = Boolean.getBoolean(PreferenceConstants.PREF_PY4J_ACTIVE); // They can override the default using -DPREF_PY4J_ACTIVE=...
		if (!enabled && !override) {
			return;
		}

		// We override any port setting because we record the auto-generated one.
		store.setValue(PreferenceConstants.PREF_DEFAULT_PORT, GatewayServer.DEFAULT_PORT);
		store.setValue(PreferenceConstants.PREF_DEFAULT_CALLBACK_PORT, GatewayServer.DEFAULT_PYTHON_PORT);

		int defaultPort = GatewayServer.DEFAULT_PORT;
		int defaultCallBackPort = GatewayServer.DEFAULT_PYTHON_PORT;
		if (!isPortFree(defaultPort)) { // find and allocate free ports in pairs
			defaultPort = getFreePort(defaultPort, 2);
			store.setValue(PreferenceConstants.PREF_DEFAULT_PORT, defaultPort);
			defaultCallBackPort = getFreePort(defaultPort + 1, 1);
			store.setValue(PreferenceConstants.PREF_DEFAULT_CALLBACK_PORT, defaultCallBackPort);
		} else if (!isPortFree(defaultCallBackPort)) {
			defaultCallBackPort = getFreePort(defaultCallBackPort, 2);
			store.setValue(PreferenceConstants.PREF_DEFAULT_CALLBACK_PORT, defaultCallBackPort);
		}

		if (store.getBoolean(PreferenceConstants.PREF_USE_SWT_DISPLAY_THREAD)) {
			server = new SWTGatewayServer(this, defaultPort, defaultCallBackPort,
					GatewayServer.DEFAULT_CONNECT_TIMEOUT,
					GatewayServer.DEFAULT_READ_TIMEOUT, null);
		} else {
			server = new GatewayServer(this, defaultPort, defaultCallBackPort,
					GatewayServer.DEFAULT_CONNECT_TIMEOUT,
					GatewayServer.DEFAULT_READ_TIMEOUT, null);
		}

		try {
			server.start();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Attempts to get a free port starting at the passed in port and working
	 * up.
	 * 
	 * TODO move this to the gatewayserver (a property that would tell the
	 * gateway server to bind to the next available port)
	 * 
	 * @param startPort
	 * @param increment
	 * @return
	 */
	public static int getFreePort(final int startPort, final int increment) {

		int port = startPort;
		while (!isPortFree(port)) {
			port += increment;
		}

		return port;
	}

	/**
	 * Checks if a port is free.
	 * 
	 * @param port
	 * @return
	 */
	public static boolean isPortFree(int port) {

		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
		} finally {
			if (ds != null) {
				ds.close();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		if (server != null) {
			server.shutdown();
		}
		context = null;
		activator = null;
		super.stop(bundleContext);
	}

	/**
	 * Might be null
	 * 
	 * @return
	 */
	public GatewayServer getServer() {
		return server;
	}

	public static DefaultServerActivator getDefault() {
		return activator;
	}

	public void closeEclipse() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().close();
			}
		});
	}
}
