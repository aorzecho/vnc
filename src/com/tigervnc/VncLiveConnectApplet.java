package com.tigervnc;

public class VncLiveConnectApplet extends Applet2 {

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
		System.out.println("Starting vnc ...");
		
		VncViewer viewer = new VncViewer(new String[] { 
				"host", host, 
				"port", port,
				"window_title", window_title, 
				"show_controls", show_controls,
				"new_window", "no",
				"log_level", log_level
		});
		
		// the only reason to do so is that system.exit shuts down
		// FF and Safari on the Mac.
		//VncViewer.inAnApplet = true;
		toFront();
	}
	
	// Bring the applet to the front.
	private void toFront(){
		setVisible(true);
		requestFocus();
	}
}
