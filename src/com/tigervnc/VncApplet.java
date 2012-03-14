package com.tigervnc;

import java.applet.Applet;

import org.apache.log4j.Logger;

public class VncApplet extends Applet2 {

	protected static Logger logger = Logger.getLogger(VncViewer.class);
	
	private String window_title;
	private String port;
	private String host;
	private String log_level;
	private String show_controls;
	private String id;

	@Override
	public void init() {
		super.init();
		port = getRequiredParameter("port");
		host = getRequiredParameter("host");
		id = getRequiredParameter("id");
		window_title = getParameter("title", "Remote Desktop Viewer");
		log_level = getParameter("log_level", "info");
		show_controls = getParameter("show_controls", "no");
		
		subscribe_to_vnc_events();
		
		startVNC();
		publishEvent(VncEvent.INIT, id);
	}
	
	private void subscribe_to_vnc_events(){
		VncEventPublisher.subscribe(new VncEventSubscriber(){
			
			@Override
			public void connectionError(String msg, Exception e){
				publishEvent(VncEvent.CONNECTION_ERROR, id, msg);
			}
			
			@Override
			public void destroy(String msg){
				publishEvent(VncEvent.DESTROY, id, msg);
			}
		});
	}

	public void startVNC() {
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
		VncViewer.applet = this;
		toFront();
	}
	
	// Bring the applet to the front.
	private void toFront(){
		setVisible(true);
		requestFocus();
	}
}
