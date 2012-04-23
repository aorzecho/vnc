package com.tigervnc;


import java.applet.Applet;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import javax.swing.JApplet;
import com.tigervnc.log.VncLogger;

public class VncApplet extends JApplet {

	private static VncLogger logger = VncLogger.getLogger(VncApplet.class);

	private String id;
	private String callback;
	private Thread jsExecutorThread;
	private	BlockingDeque<String> jsScriptQueue = new LinkedBlockingDeque<String>(10);
	private JsExecutor jsExecutor;
	public VncViewer vncViewer;

	private static class JsExecutor implements Runnable {
		
		private final Applet applet;
		private final BlockingDeque<String> scriptQueue;
		Method getWindowMethod;//this is to avoid dependency on plugin lib...
		Method evalMethod;
		Class jsObjClazz;
		
		public JsExecutor (Applet applet, BlockingDeque<String> scriptQueue) {
			this.applet = applet;
			this.scriptQueue = scriptQueue;
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

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String script = scriptQueue.takeFirst();
					logger.debug("calling javascript: " + script);
					 applet.getAppletContext().showDocument
						(new URL("javascript: " + script)); // does not work with icedtea plugin, but does not hang...
//					synchronized (jsObjClazz) {// hangs after several calls (icedtea plugin)
//						evalMethod.invoke(getWindowMethod.invoke(null, applet), "javascript: " + script + ";\n");
//					}
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
		id = getRequiredParameter("id");
		
		subscribe_to_vnc_events();

		jsExecutor = new JsExecutor(this, jsScriptQueue);
		jsExecutorThread = new Thread(jsExecutor);
		jsExecutorThread.setName("jsExecutorThread");
		jsExecutorThread.setDaemon(true);
		jsExecutorThread.start();
		
		startVNC();
		publishEvent(VncEvent.INIT, id);
	}
	
	@Override
	public void destroy() {
		vncViewer.destroy();
		jsExecutorThread.interrupt();
		super.destroy();
	}

	@Override
	public void stop() {
		vncViewer.stop();
		super.stop();
	}

	@Override
	public void start() {
		vncViewer.start();
		super.start();
	}

	private void subscribe_to_vnc_events() {
		VncEventPublisher.subscribe(new VncEventSubscriber(){
			
			@Override
			public void connectionError(String msg, Exception e){
				publishEvent(VncEvent.CONNECTION_ERROR, id, msg);
			}

			@Override
			public void updSetup(String msg) {
				publishEvent(VncEvent.UPD_SETUP, id, msg);
			}
			
			@Override
			public void destroy(String msg){
				// do not call js when applet is beeing destroyed
//				publishEvent(VncEvent.DESTROY, id, msg);
				
			}

		});
	}

	public void startVNC() {
		// the only reason to do so is that system.exit shuts down
		// FF and Safari on the Mac.
		VncViewer.inAnApplet = true;
		VncViewer.applet = this;
		vncViewer = new VncViewer(null);

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
		if (!jsScriptQueue.offerLast(script)) {
			throw new IllegalStateException("javascript queue full");
		}
	}
}
