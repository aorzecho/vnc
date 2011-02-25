package com.tigervnc;

import java.applet.Applet;

public class VncApplet extends Applet {

	private String window_title;
	private String port;
	private String host;
	private String log_level;

	public void init() {
		port = getRequiredParameter("port");
		host = getRequiredParameter("host");
		window_title = getParameter("title", "Remote Desktop Viewer");
		log_level = getParameter("log_level", "info");
		startVNC();
	}

	public void startVNC() {
		System.out.println("Starting vnc ...");
		VncViewer.main(new String[] { 
				"host", host, 
				"port", port,
				"window_title", window_title, 
				"show_controls", "no",
				"new_window", "no",
				"log_level", log_level
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

    protected String getParameter(String name, String default_value){
		String value = getParameter(name);
        if(value == null){
            return default_value;
        }
        return value;
	}

}
