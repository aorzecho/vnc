package com.tigervnc;


import java.applet.Applet;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JApplet;
import org.apache.log4j.Logger;

public class VncApplet extends JApplet {

	private static Logger logger = Logger.getLogger(VncApplet.class);

	private String window_title;
	private String port;
	private String host;
	private String log_level;
	private String show_controls;
	private String new_window;
	private String id;
	//	private JSObject js;
	private String callback;
	private Thread jsExecutorThread;
	private	BlockingQueue<String> jsScriptQueue = new ArrayBlockingQueue<String>(10);
	private JsExecutor jsExecutor;
	public VncViewer vncViewer;
	
	private static class JsExecutor implements Runnable {
		
		private final Applet applet;
		private final BlockingQueue<String> scriptQueue;
		static Method getWindowMethod;
		static Method evalMethod;
		static Class jsObjClazz;
		
		static {
			try {
				jsObjClazz = Class.forName("netscape.javascript.JSObject");
				synchronized (jsObjClazz) {
					for (Method m : jsObjClazz.getMethods()) {
						if ("eval".equals(m.getName())) {
							evalMethod = m;
						} else if (("getWindow").equals(m.getName())) {
							getWindowMethod = m;
						}
					}
				}
			} catch (Exception e) {
				logger.error("Exception while initializing JsExecutor " + e);
			}
		}
		
		public JsExecutor (Applet applet, BlockingQueue<String> scriptQueue) {
			this.applet = applet;
			this.scriptQueue = scriptQueue;
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String script = scriptQueue.take();
					logger.debug("calling javascript: " + script);
					synchronized (jsObjClazz) {// hangs after several calls (icedtea plugin)
						evalMethod.invoke(getWindowMethod.invoke(null, applet), "javascript: " + script);
					}
					logger.debug("executed javascript: " + script);
				} catch (InterruptedException ex) {
					break;
				} catch (Exception e) {
					logger.error("Got exception while performing javascript call", e);
				}
			}
		}
	}
	
	
	@Override
	public void init() {
		callback = getRequiredParameter("callback");
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
		jsExecutor = new JsExecutor(this, jsScriptQueue);
		jsExecutorThread = new Thread(jsExecutor);
		jsExecutorThread.setName("jsExecutorThread");
		jsExecutorThread.setDaemon(true);
		jsExecutorThread.start();
	}
	
        @Override
        public void destroy () {
            vncViewer.destroy();
			jsExecutorThread.interrupt();
            super.destroy();
        }
        
        @Override
        public void stop () {
            vncViewer.stop();
            super.stop();
        }
        
        @Override
        public void start () {
            vncViewer.start();
            super.start();
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
		vncViewer = new VncViewer(new String[]{
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

            evalJs("setViewportSize(" + x + "," + y + ")");

    }

	protected String getParameter(String name, String default_value) {
		String value = getParameter(name);
		if (value == null) {
			return default_value;
		}
		return value;
	}

	protected String getRequiredParameter(String name) {
		String value = getParameter(name);
		if (value == null) {
			throw new RuntimeException("Missing required parameter: " + name);
		}
		return value;
	}

	protected void publishEvent(Object event, Object... args) {
		String js_args = "'" + event + "'";
		for (Object a : args) {
			js_args += ", '" + a.toString() + "'";
		}
		evalJs(callback + "(" + js_args + ")");
	}

	public void evalJs(String script) {
		if (!jsScriptQueue.offer(script)) {
			throw new IllegalStateException("javascript queue full");
		}
	}
}
