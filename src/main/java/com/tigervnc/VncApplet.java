package com.tigervnc;


import java.applet.Applet;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import org.apache.log4j.Logger;

public class VncApplet extends Applet2 {

	private String window_title;
	private String port;
	private String host;
	private String log_level;
	private String show_controls;
	private String new_window;
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
		new_window = getParameter("new_window", "yes");
		
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
		// the only reason to do so is that system.exit shuts down
		// FF and Safari on the Mac.
		VncViewer.inAnApplet = true;
		VncViewer.applet = this;

                VncViewer.main(new String[] { 
				"host", host, 
				"port", port,
				"window_title", window_title, 
				"show_controls", show_controls,
				"new_window", new_window,
				"log_level", log_level
		});
		
		toFront();
	}
	
	// Bring the applet to the front.
	private void toFront(){
		setVisible(true);
		requestFocus();
	}
        
        
    public void resizeBrowserWindow(int x, int y) {
        try {
            System.out.println("Calling javascript function setViewportSize(" + x + "," + y + ")");

            Class jsObjClazz = Class.forName("netscape.javascript.JSObject");
            Method eval = null;
            Object window = jsObjClazz.getMethod("getWindow", Applet.class).invoke(null, this);
            for (Method m : window.getClass().getMethods()) {
                if ("eval".equals(m.getName())) {
                    eval = m;
                }
            }
            eval.invoke(window, "setViewportSize(" + x + "," + y + ")");

            System.out.println("Called javascript function setViewportSize(" + x + "," + y + ")");
        } catch (Exception e) {
            System.out.println("Got exception while performing javascript call " + e);
        }
    }

}
