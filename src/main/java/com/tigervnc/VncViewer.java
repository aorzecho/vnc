//
//  Copyright (C) 2012 ProfitBricks GmbH. All Rights Reserved.
//  Copyright (C) 2001-2004 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2002 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//

//
// VncViewer.java - the VNC viewer applet.  This class mainly just sets up the
// user interface, leaving it to the VncCanvas to do the actual rendering of
// a VNC desktop.
//

package com.tigervnc;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.tigervnc.log.VncLogger;

import com.tigervnc.rfb.Encodings;
import com.tigervnc.rfb.RfbProto;
import com.tigervnc.rfb.message.ApplyKbFixAction;
import com.tigervnc.rfb.message.KeyEntry;
import com.tigervnc.rfb.message.KeyboardEvent;
import com.tigervnc.rfb.message.KeyboardEventMap;
import com.tigervnc.ui.AuthPanel;
import com.tigervnc.ui.OptionsFrame;
import com.tigervnc.ui.ReloginPanel;
import com.tigervnc.ui.VncCanvas;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import javax.swing.*;

public class VncViewer implements java.lang.Runnable,
		WindowListener, ComponentListener {

	public static final VncLogger logger = VncLogger.getLogger(VncViewer.class);
	public static final ResourceBundle labels = ResourceBundle.getBundle("LabelsBundle");
	public static final ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle");

	public final Session session = new Session();
	public final VncEventPublisher eventPublisher = new VncEventPublisher();
	public final boolean inAnApplet;
	public final VncApplet applet;
	//
	// main() is called when run as a java program from the command line.
	// It simply runs the applet inside a newly-created frame.
	//

	public static class Session {
		// Set from the main vnc response loop VncViewer, refactor that...
		public boolean extended_key_event = false;
		public boolean _alt_gr_pressed = false;
		
		public KeyboardEventMap kbEvtMap;

		/** Maps from keycode to keysym for all currently pressed keys. */
		public Map<Integer, Integer> keys_pressed = new HashMap<Integer, Integer>();

		/** Get all pressed keys as a map from keycode to keysym */
		public Map<Integer, Integer> getPressedKeys() throws IOException {
			return keys_pressed;
		}

		/** Clear all pressed keys */
		public void clearPressedKeys() {
			keys_pressed.clear();
		}
		
	}
	

	public static void main(String[] argv) {
		new VncViewer(argv);
	}

	public String[] mainArgs;
	public RfbProto rfb;
	
	Thread rfbThread;

	public JFrame vncFrame;
	public Container vncContainer;
	public LayoutManager containerDefaultLayout;
	public VncCanvas vncCanvas;
	public Panel canvasPanel;
	public OptionsFrame options;
	public JMenu sendKeyMenu;

	String cursorUpdatesDef;
	String eightBitColorsDef;

	// Variables read from parameter values.
	public String socketFactory;
	public String host;
	public String windowTitle;
	public String keyboardSetup;
	public int port;
	public String log_level;
	public String passwordParam;
	public boolean showControls;
	public boolean separateWindow;
	public boolean offerRelogin;
	public boolean showOfflineDesktop;
	public int deferScreenUpdates;
	public int deferCursorUpdates;
	public int deferUpdateRequests;
	public int debugStatsExcludeUpdates;
	public int debugStatsMeasureUpdates;
	
	public VncViewer(VncApplet applet, VncEventSubscriber evtSubscriber) {
		this.applet = applet;
		eventPublisher.subscribe(evtSubscriber);
		inAnApplet = true;
		readParameters();
		setLogLevel(log_level);
		session.kbEvtMap = new KeyboardEventMap(eventPublisher, keyboardSetup);
		initFrame();
	}

	public VncViewer(String[] argv) {
		applet=null;
		inAnApplet = false;
		mainArgs = argv;
		readParameters();
		setLogLevel(log_level);
		session.kbEvtMap = new KeyboardEventMap(eventPublisher, keyboardSetup);
		initFrame();
	}
	
	private void initFrame() {
		if (separateWindow) {
			vncFrame = new JFrame("TigerVNC");
			vncFrame.setResizable(false);
			vncContainer = vncFrame.getContentPane();
			vncFrame.setJMenuBar(createMenuBar());
			vncFrame.addWindowListener(this);
			vncFrame.addComponentListener(this);
//		    vncFrame.addFocusListener(this);
			if (inAnApplet) {
				Panel gridPanel = new Panel(new GridLayout(0, 1));
				Panel outerPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
				outerPanel.add(gridPanel);
				applet.getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT, 30, 16));
				applet.getContentPane().add(outerPanel);

				gridPanel.add(button("requestFocus"));
			}
		} else {
			vncContainer = applet.getContentPane();
			applet.setJMenuBar(createMenuBar());
//			applet.addComponentListener(this);
//		    applet.addFocusListener(this);
		}
		containerDefaultLayout = vncContainer.getLayout();

		options = new OptionsFrame(this);
		if (showControls) {
			options.setVisible(true);
		}

		ToolTipManager.sharedInstance().setDismissDelay(25000);
		
		connect();
	}

	public void pack() {
		if (separateWindow) {
			vncFrame.pack();
		} else {
			applet.validate();
			if (vncCanvas != null) {
				applet.resizeBrowserWindow(
						vncCanvas.getWidth(),
						vncCanvas.getHeight() + applet.getJMenuBar().getHeight());
			}
		}
	}

	private void setTitle(String title) {
		if (separateWindow) {
			vncFrame.setTitle(title);
		}
	}

	private void show() {
		if (separateWindow) {
			vncFrame.setVisible(true);
		} else {
			applet.setVisible(true);
		}
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(button("disconnect"));
		menuBar.add(button("toggleOptions"));
		menuBar.add(button("refresh"));

		menuBar.add(createKbSetupMenu());
		menuBar.add(createSendKeyMenu());
		return menuBar;
	}
	
	private JMenu createKbSetupMenu() {
		JMenu kbMenu = localize(new JMenu(), "menu.keyboard");;
		for (String group : session.kbEvtMap.manualFixes.keySet()) {
			List<ApplyKbFixAction> fixes = session.kbEvtMap.manualFixes.get(group);
			if ("".equals(group) || fixes.size() == 1) { // checkboxes
				for (ApplyKbFixAction action : fixes) {
					kbMenu.add(localize(new JCheckBoxMenuItem(action), "keyboardfix." + action.fix.id))
									.setSelected(action.isApplied());
				}
			} else {
				ButtonGroup bg = new ButtonGroup();
				JRadioButtonMenuItem item = localize(new JRadioButtonMenuItem(), "group." + group);
				kbMenu.add(item).setSelected(true);
				bg.add(item);
				for (ApplyKbFixAction action : fixes) {
					item = localize(new JRadioButtonMenuItem(action), "keyboardfix." + action.fix.id);
					item.addChangeListener(action);
					kbMenu.add(item);
					bg.add(item);
					bg.setSelected(item.getModel(), action.isApplied());
				}
			}
			kbMenu.add(new JSeparator());
		}
		return kbMenu;
	}
	
	private JMenu createSendKeyMenu() {
		sendKeyMenu = localize(new JMenu(), "menu.sendKey");
		sendKeyMenu.setEnabled(false); //set in Options if input is enabled
		sendKeyMenu.add(keyItem("VK_DELETE+ALT+CTRL"));
		sendKeyMenu.add(keyItem("VK_TAB+ALT"));
		sendKeyMenu.add(keyItem("VK_ESCAPE+CTRL"));
		sendKeyMenu.add(keyItem("VK_BACK_SPACE+ALT+CTRL"));
		sendKeyMenu.add(keyItem("VK_F1+ALT+CTRL"));
		sendKeyMenu.add(keyItem("VK_F7+ALT+CTRL"));
		sendKeyMenu.add(menuItem("releaseModKeys","releaseModKeys"));
		    
		return sendKeyMenu;
	}

	private JButton button(String action, Object... args) {
		JButton btn = localize(new JButton(), "button." + action);
		try {
			btn.addActionListener(new InvokeAction(this, action, args));
		} catch (NoSuchMethodException ex) {
			logger.error("Unable to register " + action + " action", ex);
		}
		return btn;
	}

	private JMenuItem keyItem (String key) {
		return menuItem(key,"sendKey", new KeyEntry(key));
	}
	
	private JMenuItem menuItem(String label, String action, Object ... args) {
		JMenuItem item = localize(new JMenuItem(label), "button." + label);
		try {
			item.addActionListener(new InvokeAction(this, action, args));
		} catch (NoSuchMethodException ex) {
			logger.error("Unable to register " + action + " action", ex);
		}
		return item;
	}

	public void update(Graphics g) {}

	public void requestFocus () {
		if (vncFrame == null) {
			initFrame();
		} else {
			vncFrame.setState(Frame.NORMAL);
			vncFrame.toFront();
			vncFrame.requestFocus();
		}
		moveFocusToDesktop();
	}
	
	public void toggleOptions() {
		options.setVisible(!options.isVisible());
	}

	public void refresh() {
		if (rfb != null && !rfb.closed()) {
			try {
				rfb.writeFramebufferUpdateRequest(0, 0, rfb.server.fb_width, rfb.server.fb_height, false);
			} catch (IOException ex) {
				logger.error("Exception sending refresh request", ex);
			}
		}
	}

	public void releaseModKeys() {
			for (KeyEntry key : KeyEntry.MODIFIER_KEYS)
				sendKey(key);
	}
	
	public void sendKey(KeyEntry keyReq) {
		if (rfb != null && !rfb.closed()) try {

			KeyEntry key = session.kbEvtMap.remapCodes(new KeyEvent(vncContainer, KeyEvent.KEY_PRESSED, 0,
							keyReq.getModifierMask(), keyReq.keycode, (char) keyReq.keysym));
			for (KeyboardEventMap.EvtEntry evt : key.getExtendedWriteEvents()) {
				rfb.writeKeyboardEvent(evt.key.keysym, evt.key.keycode, evt.evtId == KeyEvent.KEY_PRESSED);
			}

		} catch (Exception ex) {
			logger.error("Exception sending " + keyReq, ex);
		}
	}

	//
	// run() - executed by the rfbThread to deal with the RFB socket.
	//
	public void run() {

		try {
			connectAndAuthenticate();
			doProtocolInitialisation();
			options.updateState();
			createCanvas(0, 0);

			// Create a panel which itself is resizeable and can hold
			// non-resizeable VncCanvas component at the top left corner.
			canvasPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			canvasPanel.add(vncCanvas);

			// If auto scale is not enabled we don't need to set first frame
			// size to fullscreen
			if (!options.autoScale) {
				vncCanvas.isFirstSizeAutoUpdate = false;
			}

			// Finally, add our panel to the Frame window.
			setTitle(windowTitle);
			pack();

			moveFocusToDesktop();
			processNormalProtocol();

		} catch (NoRouteToHostException e) {
			fatalError("Network error: no route to server: " + host, e);
		} catch (UnknownHostException e) {
			fatalError("Network error: server name unknown: " + host, e);
		} catch (ConnectException e) {
			String msg = "Network error: could not connect to server: " + host+ ":" + port;
			eventPublisher.publish(VncEvent.CONNECTION_ERROR, msg, e);
			onRfbDisconnected();
		} 
		catch (EOFException e) {
			if (showOfflineDesktop) {
				e.printStackTrace();
				System.out
						.println("Network error: remote side closed connection");
				if (vncCanvas != null) {
					vncCanvas.enableInput(false);
				}
				setTitle(windowTitle + " [disconnected]");
				
				if (rfb != null && !rfb.closed())
					rfb.close();
			} else {
				fatalError("Network error: remote side closed connection", e);
			}
		} catch (IOException e) {
			String str = e.getMessage();
			if (str != null && str.length() != 0) {
				fatalError("Network Error: " + str, e);
			} else {
				fatalError(e.toString(), e);
			}
		} catch (Exception e) {
			String str = e.getMessage();
			if (str != null && str.length() != 0) {
				fatalError("Error: " + str, e);
			} else {
				fatalError(e.toString(), e);
			}
		}
	}

	//
	// Create a VncCanvas instance.
	//
	void createCanvas(int maxWidth, int maxHeight) throws IOException {
		// Determine if Java 2D API is available and use a special
		// version of VncCanvas if it is present.
		vncCanvas = null;
		try {
			// This throws ClassNotFoundException if there is no Java 2D API.
			Class cl = Class.forName("java.awt.Graphics2D");
			// If we could load Graphics2D class, then we can use VncCanvas2D.
			cl = Class.forName("com.tigervnc.vncviewer.VncCanvas2");
			Class[] argClasses = { this.getClass(), Integer.TYPE, Integer.TYPE };
			java.lang.reflect.Constructor cstr = cl.getConstructor(argClasses);
			Object[] argObjects = { this, new Integer(maxWidth),
					new Integer(maxHeight) };
			vncCanvas = (VncCanvas) cstr.newInstance(argObjects);
		} catch (Exception e) {
			logger.warn("Java 2D API is not available");
		}

		// If we failed to create VncCanvas2D, use old VncCanvas.
		if (vncCanvas == null)
			vncCanvas = new VncCanvas(this, maxWidth, maxHeight);
	}

	//
	// Process RFB socket messages.
	// If the rfbThread is being stopped, ignore any exceptions,
	// otherwise rethrow the exception so it can be handled.
	//

	void processNormalProtocol() throws Exception {
		try {
			vncCanvas.processNormalProtocol();
		} 
		catch (Exception e) {
			if (rfbThread == null) {
				logger.info("Ignoring RFB socket exceptions"
						+ " because vncViewer is stopping");
			} else {
				throw e;
			}
		}
	}

	//
	// Connect to the RFB server and authenticate the user.
	//

	void connectAndAuthenticate() throws Exception {
		showConnectionStatus("Initializing...");
		
		pack();
		show();

		showConnectionStatus("Connecting to " + host + ", port " + port + "...");

		rfb = new RfbProto(host, port, this);
		showConnectionStatus("Connected to server");

		rfb.readVersionMsg();
		showConnectionStatus("RFB server supports protocol version "
				+ rfb.serverVersion);

		rfb.writeVersionMsg();
		showConnectionStatus("Using RFB protocol version " + rfb.clientVersion);

		int secType = rfb.negotiateSecurity();
		int authType;
		if (secType == Encodings.SecTypeTight) {
			showConnectionStatus("Enabling TightVNC protocol extensions");
			rfb.setupTunneling();
			authType = rfb.negotiateAuthenticationTight();
		} else {
			authType = secType;
		}

		switch (authType) {
		case Encodings.AuthNone:
			showConnectionStatus("No authentication needed");
			rfb.authenticateNone();
			break;
		case Encodings.AuthVNC:
			showConnectionStatus("Performing standard VNC authentication");
			if (passwordParam != null) {
				rfb.authenticateVNC(passwordParam);
			} else {
				String pw = askPassword();
				rfb.authenticateVNC(pw);
			}
			break;
		default:
			throw new Exception("Unknown authentication scheme " + authType);
		}
	}

	//
	// Show a message describing the connection status.
	// To hide the connection status label, use (msg == null).
	//

	void showConnectionStatus(String msg) {
		return;
	}

	//
	// Show an authentication panel.
	//
	void doAuthentification(int secType) throws Exception {
		switch (secType) {
		case Encodings.SecTypeNone:
			showConnectionStatus("No authentication needed");
			rfb.authenticateNone();
			break;
		case Encodings.SecTypeVncAuth:
			showConnectionStatus("Performing standard VNC authentication");
			if (passwordParam != null) {
				rfb.authenticateVNC(passwordParam);
			} else {
				String pw = askPassword();
				rfb.authenticateVNC(pw);
			}
			break;
		case Encodings.SecTypeVeNCrypt:
			showConnectionStatus("VeNCrypt chooser");
			secType = rfb.authenticateVeNCrypt();
			doAuthentification(secType);
			break;
		case Encodings.SecTypePlain:
			showConnectionStatus("Plain authentication");
			{
				String user = askUser();
				String pw = askPassword();
				rfb.authenticatePlain(user, pw);
			}
			break;
		default:
			throw new Exception("Unknown authentication scheme " + secType);
		}
	}

	String askPassword() throws Exception {
		showConnectionStatus(null);

		AuthPanel authPanel = new AuthPanel(this, true);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.ipadx = 100;
		gbc.ipady = 50;

		canvasPanel.add(authPanel);
		pack();
		
		authPanel.moveFocusToDefaultField();
		String pw = authPanel.getPassword();
		canvasPanel.remove(authPanel);

		return pw;
	}

	//
	// Do the rest of the protocol initialisation.
	//

	void doProtocolInitialisation() throws IOException {
		rfb.writeClientInit();
		rfb.readServerInit();

		logger.info("Desktop size is " + rfb.server.fb_width + " x "
				+ rfb.server.fb_height);
		
	}

	String askUser() throws Exception {

		showConnectionStatus(null);

		AuthPanel authPanel = new AuthPanel(this, false);
		canvasPanel.add(authPanel);
		
		pack();

		authPanel.moveFocusToDefaultField();
		String pw = authPanel.getPassword();
		canvasPanel.remove(authPanel);

		return pw;
	}

	//
	// Send current encoding list to the RFB server.
	//

	List<Integer> encodingsSaved = new ArrayList<Integer>();
	int nEncodingsSaved;

	public void setEncodings() {
		setEncodings(false);
	}

	public void autoSelectEncodings() {
		setEncodings(true);
	}

	void setEncodings(boolean autoSelectOnly) {
		if (options == null || rfb == null || !rfb.inNormalProtocol)
			return;

		int preferredEncoding = options.preferredEncoding;
		if (preferredEncoding == -1) {
			long kbitsPerSecond = rfb.kbitsPerSecond();
			if (nEncodingsSaved < 1) {
				// Choose Tight or ZRLE encoding for the very first update.
				logger.info("Using Tight/ZRLE encodings");
				preferredEncoding = Encodings.EncodingTight;
			} else if (kbitsPerSecond > 2000
					&& encodingsSaved.get(0) != Encodings.EncodingHextile) {
				// Switch to Hextile if the connection speed is above 2Mbps.
				logger.info("Throughput " + kbitsPerSecond
						+ " kbit/s - changing to Hextile encoding");
				preferredEncoding = Encodings.EncodingHextile;
			} else if (kbitsPerSecond < 1000
					&& encodingsSaved.get(0) != Encodings.EncodingTight) {
				// Switch to Tight/ZRLE if the connection speed is below 1Mbps.
				logger.info("Throughput " + kbitsPerSecond
						+ " kbit/s - changing to Tight/ZRLE encodings");
				preferredEncoding = Encodings.EncodingTight;
			} else {
				// Don't change the encoder.
				if (autoSelectOnly)
					return;
				preferredEncoding = encodingsSaved.get(0);
			}
		} else {
			// Auto encoder selection is not enabled.
			if (autoSelectOnly)
				return;
		}

		ArrayList<Integer> encodings = new ArrayList<Integer>();

		encodings.add(preferredEncoding);
		if (options.useCopyRect) {
			encodings.add(Encodings.EncodingCopyRect);
		}

		if (preferredEncoding != Encodings.EncodingTight) {
			encodings.add(Encodings.EncodingTight);
		}
		if (preferredEncoding != Encodings.EncodingZRLE) {
			encodings.add(Encodings.EncodingZRLE);
		}
		if (preferredEncoding != Encodings.EncodingHextile) {
			encodings.add(Encodings.EncodingHextile);
		}
		if (preferredEncoding != Encodings.EncodingZlib) {
			encodings.add(Encodings.EncodingZlib);
		}
		if (preferredEncoding != Encodings.EncodingCoRRE) {
			encodings.add(Encodings.EncodingCoRRE);
		}
		if (preferredEncoding != Encodings.EncodingRRE) {
			encodings.add(Encodings.EncodingRRE);
		}

		if (options.compressLevel >= 0 && options.compressLevel <= 9) {
			encodings.add(Encodings.EncodingCompressLevel0
					+ options.compressLevel);
		}
		if (options.jpegQuality >= 0 && options.jpegQuality <= 9) {
			encodings
					.add(Encodings.EncodingQualityLevel0 + options.jpegQuality);
		}

		if (options.requestCursorUpdates) {
			encodings.add(Encodings.EncodingXCursor);
			encodings.add(Encodings.EncodingRichCursor);
			if (!options.ignoreCursorUpdates)
				encodings.add(Encodings.EncodingPointerPos);
		}

		encodings.add(Encodings.ENCODING_EXTENDED_KEY_EVENT);
		encodings.add(Encodings.EncodingLastRect);
		encodings.add(Encodings.EncodingNewFBSize);

		boolean encodingsWereChanged = false;
		if (encodings.size() != nEncodingsSaved) {
			encodingsWereChanged = true;
		} else {
			for (int i = 0; i < encodings.size(); i++) {
				if (encodings.get(i) != encodingsSaved.get(i)) {
					encodingsWereChanged = true;
					break;
				}
			}
		}

		if (encodingsWereChanged) {
			try {
				rfb.writeSetEncodings(encodings);
				if (vncCanvas != null) {
					vncCanvas.softCursorFree();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			encodingsSaved = (ArrayList<Integer>) encodings.clone();
			nEncodingsSaved = encodings.size();
		}
	}

	//
	// setCutText() - send the given cut text to the RFB server.
	//

	public void setCutText(String text) {
		try {
			if (rfb != null && rfb.inNormalProtocol) {
				rfb.writeClientCutText(text);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	//
	// readParameters() - read parameters from the html source or from the
	// command line. On the command line, the arguments are just a sequence of
	// param_name/param_value pairs where the names and values correspond to
	// those expected in the html applet tag source.
	//
	
	private String getParameter(String name, String default_value){
		String value = readParameter(name, false);
		return value != null ? value : default_value;
	}
	
	private void setLogLevel(String log_level){
		switch (log_level.charAt(0)) {
		case 'd':
		case 'D':
			VncLogger.setDefaultLevel(Level.FINE);
			break;
		case 'i':
		case 'I':
			VncLogger.setDefaultLevel(Level.INFO);
			break;
		case 'w':
		case 'W':
			VncLogger.setDefaultLevel(Level.WARNING);
			break;
		case 'e':
		case 'E':
			VncLogger.setDefaultLevel(Level.SEVERE);
			break;
		case 'f':
		case 'F':
			VncLogger.setDefaultLevel(Level.SEVERE);
			break;
		default:
			System.err.println(": Invalid debug level: "
					+ log_level.charAt(0));
		}
	}

	void readParameters() {
		host = readParameter("host", true);
		port = readIntParameter("port", 5900);
		log_level = getParameter("log_level", "info");
		keyboardSetup = getParameter("keyboard_setup", null);

		// Read "ENCPASSWORD" or "PASSWORD" parameter if specified.
		readPasswordParameters();

		windowTitle = readParameter("window_title", false);
		if (windowTitle == null) {
			windowTitle = "VncViewer";
		}

		String str;

		// "Show Controls" set to "Yes" to show controls at start.
		str = readParameter("show_controls", false);
		showControls = str != null && !str.equalsIgnoreCase("no");

		// "New window" set to "No" disable creatind separate JFrame in applet.
		separateWindow = true;
		str = readParameter("new_window", false);
		if (str != null && str.equalsIgnoreCase("No"))
			separateWindow = false;

		// "Offer Relogin" set to "No" disables "Login again" and "Close
		// window" buttons under error messages in applet mode.
		offerRelogin = true;
		str = readParameter("Offer Relogin", false);
		if (str != null && str.equalsIgnoreCase("No"))
			offerRelogin = false;

		// Do we continue showing desktop on remote disconnect?
		showOfflineDesktop = false;
		str = readParameter("Show Offline Desktop", false);
		if (str != null && str.equalsIgnoreCase("Yes"))
			showOfflineDesktop = true;

		// Fine tuning options.
		deferScreenUpdates = readIntParameter("Defer screen updates", 20);
		deferCursorUpdates = readIntParameter("Defer cursor updates", 10);
		deferUpdateRequests = readIntParameter("Defer update requests", 0);

		// Debugging options.
		debugStatsExcludeUpdates = readIntParameter("DEBUG_XU", 0);
		debugStatsMeasureUpdates = readIntParameter("DEBUG_CU", 0);

		// SocketFactory.
		socketFactory = readParameter("SocketFactory", false);
	}

	//
	// Read password parameters. If an "ENCPASSWORD" parameter is set,
	// then decrypt the password into the passwordParam string. Otherwise,
	// try to read the "PASSWORD" parameter directly to passwordParam.
	//

	private void readPasswordParameters() {
		String encPasswordParam = readParameter("ENCPASSWORD", false);
		if (encPasswordParam == null) {
			passwordParam = readParameter("PASSWORD", false);
		} else {
			// ENCPASSWORD is hexascii-encoded. Decode.
			byte[] pw = { 0, 0, 0, 0, 0, 0, 0, 0 };
			int len = encPasswordParam.length() / 2;
			if (len > 8)
				len = 8;
			for (int i = 0; i < len; i++) {
				String hex = encPasswordParam.substring(i * 2, i * 2 + 2);
				Integer x = new Integer(Integer.parseInt(hex, 16));
				pw[i] = x.byteValue();
			}
			// Decrypt the password.
			byte[] key = { 23, 82, 107, 6, 35, 78, 88, 7 };
			DesCipher des = new DesCipher(key);
			des.decrypt(pw, 0, pw, 0);
			passwordParam = new String(pw);
		}
	}

	public String readParameter(String name, boolean required) {
		if (inAnApplet) {
			String s = applet.getParameter(name);
			if ((s == null) && required) {
				fatalError(name + " parameter not specified");
			}
			return s;
		}

		for (int i = 0; i < mainArgs.length; i += 2) {
			if (mainArgs[i].equalsIgnoreCase(name)) {
				try {
					return mainArgs[i + 1];
				} catch (Exception e) {
					if (required) {
						fatalError(name + " parameter not specified");
					}
					return null;
				}
			}
		}
		if (required) {
			fatalError(name + " parameter not specified");
		}
		return null;
	}

	public int readIntParameter(String name, int defaultValue) {
		String str = readParameter(name, false);
		int result = defaultValue;
		if (str != null) {
			try {
				result = Integer.parseInt(str);
			} catch (NumberFormatException e) {
			}
		}
		return result;
	}

	//
	// moveFocusToDesktop() - move keyboard focus either to VncCanvas.
	//

	public void moveFocusToDesktop() {
		if (vncContainer != null) {
			if (vncCanvas != null && vncContainer.isAncestorOf(vncCanvas))
				vncCanvas.requestFocus();
		}
	}

	//
	// disconnect() - close connection to server.
	//

	synchronized public void disconnect() {

		if (vncCanvas != null) {
			double sec = (System.currentTimeMillis() - vncCanvas.statStartTime) / 1000.0;
			double rate = Math.round(vncCanvas.statNumUpdates / sec * 100) / 100.0;
			long nRealRects = vncCanvas.statNumPixelRects;
			long nPseudoRects = vncCanvas.statNumTotalRects
					- vncCanvas.statNumPixelRects;
			logger.info("Updates received: " + vncCanvas.statNumUpdates
					+ " (" + nRealRects + " rectangles + " + nPseudoRects
					+ " pseudo), " + rate + " updates/sec");
			long numRectsOther = nRealRects - vncCanvas.statNumRectsTight
					- vncCanvas.statNumRectsZRLE
					- vncCanvas.statNumRectsHextile - vncCanvas.statNumRectsRaw
					- vncCanvas.statNumRectsCopy;
			logger.info("Rectangles:" + " Tight="
					+ vncCanvas.statNumRectsTight + "(JPEG="
					+ vncCanvas.statNumRectsTightJPEG + ") ZRLE="
					+ vncCanvas.statNumRectsZRLE + " Hextile="
					+ vncCanvas.statNumRectsHextile + " Raw="
					+ vncCanvas.statNumRectsRaw + " CopyRect="
					+ vncCanvas.statNumRectsCopy + " other=" + numRectsOther);

			long raw = vncCanvas.statNumBytesDecoded;
			long compressed = vncCanvas.statNumBytesEncoded;
			if (compressed > 0) {
				double ratio = Math.round((double) raw / compressed * 1000) / 1000.0;
				logger.info("Pixel data: "
						+ vncCanvas.statNumBytesDecoded + " bytes, "
						+ vncCanvas.statNumBytesEncoded + " compressed, ratio "
						+ ratio);
			}
		}
		if (rfb != null && !rfb.closed())
			rfb.close();
		if (rfbThread != null)
			rfbThread.interrupt();
		onRfbDisconnected();
	}

	public void onRfbDisconnected() {
		if (vncCanvas != null) {
			canvasPanel.remove(vncCanvas);
			vncContainer.remove(vncCanvas);
			vncCanvas = null;
		}
		showMessage("disconnected");
	}

	synchronized public void connect() {
		if (rfb != null && !rfb.closed())
			rfb.close();
		if (rfbThread != null && ! rfbThread.isInterrupted())
				rfbThread.interrupt();
		rfbThread = new Thread(this);
		vncContainer.removeAll();
		vncContainer.setLayout(containerDefaultLayout);
		canvasPanel = new Panel();
		vncContainer.add(canvasPanel);
		rfbThread.start();
	}

	synchronized public void close() {
		if (inAnApplet && !separateWindow) {
			disconnect();
			applet.evalJs("window.close()");
		} else {
			destroy();
		}
	}

	//
	// fatalError() - print out a fatal error message.
	// FIXME: Do we really need two versions of the fatalError() method?
	//

	synchronized public void fatalError(String str) {
		logger.error(str);
		if (inAnApplet) {
			eventPublisher.publish(VncEvent.FATAL_ERROR, str);
		} else {
			close();
		}
	}

	synchronized public void fatalError(String str, Exception e) {
		logger.error(str, e);
		
		if (rfb != null && rfb.closed()) {
			// Not necessary to show error message if the error was caused
			// by I/O problems after the rfb.close() method call.
			logger.info("RFB thread finished");
		} else {
			if (inAnApplet) {
				eventPublisher.publish(VncEvent.FATAL_ERROR, str, e);
			} else {
				close();
			}
		}

	}

	//
	// Show message text and optionally "Relogin" and "Close" buttons.
	//

	void showMessage(String msg) {
		vncContainer.removeAll();

		JLabel errLabel = new JLabel(messages.containsKey(msg) ? messages.getString(msg) : msg, JLabel.CENTER);
		errLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
		if (offerRelogin) {

			JPanel gridPanel = new JPanel(new GridLayout(0, 1));
			JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			outerPanel.add(gridPanel);
			vncContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 30, 16));
			vncContainer.add(outerPanel);
			JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			textPanel.add(errLabel);
			gridPanel.add(textPanel);
			gridPanel.add(new ReloginPanel(this));

		} else {
			vncContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 30, 30));
			vncContainer.add(errLabel);
		}
		pack();
		
	}

	public void stop() {
		logger.info("Stopping vncViewer");
	}

	public void start () {
		logger.info("Starting vncViewer");
	
	}
	
	//
	// This method is called before the vncViewer is destroyed.
	//

	public void destroy() {
		logger.info("Destroying vncViewer");
		if (rfb != null && !rfb.closed())
			rfb.close();
		if (rfbThread != null)
			rfbThread.interrupt();
		if (vncContainer != null) {
			vncContainer.removeAll();
		}
		if (vncFrame != null) {
			vncFrame.dispose();
		}
		if (options != null) {
			options.dispose();
			options = null;
		}
		if (rfbThread!=null) {
			rfbThread.interrupt();
		}
	
		vncFrame = null;
		vncContainer = null;
		containerDefaultLayout = null;
		VncCanvas vncCanvas = null;
		Panel canvasPanel = null;
		OptionsFrame options = null;
		JMenu sendKeyMenu = null;
		
		if (!inAnApplet)
			System.exit(0);
		logger.info("vncViewer destroyed");
	}


	//
	// Start/stop receiving mouse events.
	//

	public void enableInput(boolean enable) {
		vncCanvas.enableInput(enable);
	}

	//
	// Resize framebuffer if autoScale is enabled.
	//

	public void componentResized(ComponentEvent e) {
		if (e.getComponent() == vncFrame) {
			if (options.autoScale) {
				if (vncCanvas != null) {
					if (!vncCanvas.isFirstSizeAutoUpdate) {
						vncCanvas.updateFramebufferSize();
					}
				}
			}
		}
	}

	//
	// Ignore component events we're not interested in.
	//

	public void componentShown(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentHidden(ComponentEvent e) {
	}

	//
	// Close application properly on window close event.
	//

	@Override
	public void windowClosing(WindowEvent evt) {
		logger.debug("windowClosing");
		destroy();
	}
	
	@Override
	public void windowClosed(WindowEvent evt) {
		logger.debug("windowClosed");
	}

	//
	// Ignore window events we're not interested in.
	//

	public void windowActivated(WindowEvent evt) {
		logger.debug("windowActivated");
	}
	
	public void relaseAllKeys(){
		try {
			if(rfb != null){
				rfb.releaseAllKeys();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public void windowDeactivated(WindowEvent evt) {
		logger.debug("windowDeactived");
		relaseAllKeys();
	}

	public void windowOpened(WindowEvent evt) {
		logger.debug("windowOpened");
	}

	public void windowIconified(WindowEvent evt) {
		logger.debug("windowIconified");
		relaseAllKeys();
	}

	public void windowDeiconified(WindowEvent evt) {
		logger.debug("windowDeiconified");
	}

	public static <T extends AbstractButton> T localize (T component, String key) {
		if (labels.containsKey("lbl." +key))
			component.setText(labels.getString("lbl." +key));
		if (labels.containsKey("tip." + key))
			component.setToolTipText(labels.getString("tip." + key));
		return component;
	}
	

}
