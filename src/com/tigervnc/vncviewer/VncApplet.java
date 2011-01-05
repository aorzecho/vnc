package com.tigervnc.vncviewer;

import java.applet.Applet;

public class VncApplet extends Applet {

	private String window_title;
	private String port;
	private String host;

	public void init() {
		port = getRequiredParameter("port");
		host = getRequiredParameter("host");
		window_title = "VNC Example";
		System.out.println("Starting display on " + host
				+ ":" + port);
		startVNC();
	}

	public void startVNC() {
		System.out.println("Starting vnc ...");
		VncViewer.main(new String[] { 
				"host", host, 
				"port", port,
				"window_title", window_title, 
				"show_controls", "no",
				"new_window", "no"
		});
		
		// the only reason to do so is that system.exit shuts down
		// FF and Safari on the Mac.
		VncViewer.inAnApplet = true;
	}

	private String getRequiredParameter(String name) {
		String value = getParameter(name);
		if (value == null) {
			throw new RuntimeException("Missing required parameter: " + name);
		}
		return value;
	}
}
