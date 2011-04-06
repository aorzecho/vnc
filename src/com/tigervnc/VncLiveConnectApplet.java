package com.tigervnc;

import java.applet.Applet;

public class VncLiveConnectApplet extends Applet2 {

	private String window_title;
	private String port;
	private String host;
	private String log_level;
	private String show_controls;

	public void init() {
		port = getRequiredParameter("port");
		host = getRequiredParameter("host");
		window_title = getParameter("title", "Remote Desktop Viewer");
		log_level = getParameter("log_level", "info");
		show_controls = getParameter("show_controls", "no");
		startVNC();
		publishEvent(Event.INIT);
	}

	public void startVNC() {
		System.out.println("Starting vnc ...");
		VncViewer.main(new String[] { 
				"host", host, 
				"port", port,
				"window_title", window_title, 
				"show_controls", show_controls,
				"new_window", "no",
				"log_level", log_level
		});
		
		// the only reason to do so is that system.exit shuts down
		// FF and Safari on the Mac.
		VncViewer.inAnApplet = true;
		toFront();
	}
	
	// Bring the applet to the front.
	private void toFront(){
		setVisible(true);
		requestFocus();
	}
	
	private enum Event {
		INIT("Init"),
		CONNECTION_ERROR("ConnectinError");

		private String name;

		Event(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

}
