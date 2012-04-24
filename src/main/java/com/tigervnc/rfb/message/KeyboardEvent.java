package com.tigervnc.rfb.message;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tigervnc.log.VncLogger;

import com.tigervnc.Util;
import com.tigervnc.VncViewer;
import com.tigervnc.rfb.Encodings;
import com.tigervnc.rfb.RfbUtil;

public class KeyboardEvent implements IServerMessage {

	// for the values of the VK_... constants
	// http://kickjava.com/src/java/awt/event/KeyEvent.java.htm
	
	private static VncLogger logger = VncLogger.getLogger(KeyboardEvent.class);
	
	public static class KeyUndefinedException extends Exception {
		public KeyUndefinedException (String msg){
			super(msg);
		}
	}
	
	public static final int X11_BACK_SPACE = 0xff08;
	public static final int X11_TAB = 0xff09;
	public static final int X11_ENTER = 0xff0d;
	public static final int X11_ESCAPE = 0xff1b;
	public static final int X11_ALT = 0xffe9;
	public static final int X11_ALT_GRAPH = 0xff7e;
	public static final int X11_CONTROL = 0xffe3;
	public static final int X11_SHIFT = 0xffe1;
	public static final int X11_DELETE = 0xffff;
	public static final int X11_WINDOWS = 0xff20;

	protected int _keysym;
	protected int _keycode;
	protected boolean _press;
	protected List<KeyboardEvent> _extra_preceding_events;

	private boolean bypass_original_event = false;
	
	private final VncViewer.Session session;

	public KeyboardEvent(VncViewer.Session session, KeyEvent evt) throws KeyUndefinedException {
		
		this.session = session;
		KeyEntry key = session.kbEvtMap.remapCodes(evt);
		_keycode = key.keycode;
		_keysym = key.keysym;
		_press = (evt.getID() == KeyEvent.KEY_PRESSED);
                
		handleShortcuts(evt);
		handlePecularaties(evt);
		
		// only important if not using extended key events
		// the kvm vnc client ignores the keysym.
		// wonder why the keysym is required in that case.
		if(session.extended_key_event){
			handleUndefinedJavaKeysymsConvert2x11(_keycode);
		}
	}

	public KeyboardEvent(VncViewer.Session session, int keysym, int keycode, boolean down) {

		this.session = session;
		this._keysym = keysym;
		this._keycode = keycode;
		this._press = down;
		if(session.extended_key_event){
			handleUndefinedJavaKeysymsConvert2x11(keycode);
		}
	}

	/**
	 * Handles shortcuts in the client.
	 * 
	 * Ctrl-Alt-Delete = Ctrl-Alt-BackSpace
	 * 
	 * @param evt
	 * @return whether a shortcut was applied.
	 */
	final protected void handleShortcuts(KeyEvent evt) {		
		// WTF? no VK alt Gr on Windows, instead Ctrl + Alt
		// Actually just always do this, so Ctrl + Alt is Alt Gr
		if (_keycode == KeyEvent.VK_ALT) {
			if (evt.isControlDown()) {
				bypass_original_event = true;
				if(!session._alt_gr_pressed){
					session._alt_gr_pressed = true;
					// release the by user pressed control key
					addExtraEvent(X11_CONTROL,KeyEvent.VK_CONTROL, false);
					addExtraEvent(X11_ALT_GRAPH, KeyEvent.VK_ALT_GRAPH, true);
					
					// ensures that it is released when ctrl+alt is used on linux to shift to anther desktop
					session.keys_pressed.put(KeyEvent.VK_ALT_GRAPH, X11_ALT_GRAPH);
				}
			}
			else if(session._alt_gr_pressed){
				bypass_original_event = true;
				// release
				addExtraEvent(X11_ALT_GRAPH, KeyEvent.VK_ALT_GRAPH, false);	
				session._alt_gr_pressed = false;				
			}
		}
		else if(_keycode == KeyEvent.VK_CONTROL){
			if(evt.isAltDown()){
				bypass_original_event = true;
				if(!session._alt_gr_pressed){
					session._alt_gr_pressed = true;
					// release the by user pressed alt key
					addExtraEvent(X11_ALT,KeyEvent.VK_ALT, false);	
					
					addExtraEvent(X11_ALT_GRAPH, KeyEvent.VK_ALT_GRAPH, true);
					
					session.keys_pressed.put(KeyEvent.VK_ALT_GRAPH, X11_ALT_GRAPH);
				}
			}
			else if(session._alt_gr_pressed){
				bypass_original_event = true;
				// release
				addExtraEvent(X11_ALT_GRAPH, KeyEvent.VK_ALT_GRAPH, false);
				session._alt_gr_pressed = false;
			}
		}
		switch (_keycode) {
		case KeyEvent.VK_BACK_SPACE:
			if (!(evt.isAltDown() && evt.isControlDown())) {
				return;
			}
			addExtraEvent(X11_CONTROL, KeyEvent.VK_CONTROL, _press);
			addExtraEvent(X11_ALT, KeyEvent.VK_ALT, _press);
			addExtraEvent(X11_DELETE, KeyEvent.VK_DELETE, _press);
			break;
		case KeyEvent.VK_META: 		
			// No Win key on Mac use META (cmd)
			_keycode = KeyEvent.VK_WINDOWS;
			break;
		case KeyEvent.VK_DELETE:
			// re-enable ctrl-alt-delete
			if (!(evt.isAltDown() && evt.isControlDown())) {
				return;
			}
			addExtraEvent(X11_CONTROL, KeyEvent.VK_CONTROL, _press);
			addExtraEvent(X11_ALT, KeyEvent.VK_ALT, _press);
			addExtraEvent(X11_DELETE, KeyEvent.VK_DELETE, _press);
			break;
		}
	}

	private void addExtraEvent(int _keysym, int _keycode, boolean _press) {
		if (_extra_preceding_events == null) {
			// max two additional at the moment
			_extra_preceding_events = new LinkedList<KeyboardEvent>();
		}
		_extra_preceding_events.add(new KeyboardEvent(session, _keysym, _keycode, _press));
	}
	
	public byte[] getBytes() {
		return getKeyEvent();
	}

	final protected void handleUndefinedJavaKeysymsConvert2x11(int keycode) {
		switch (keycode) {
		case KeyEvent.VK_BACK_SPACE:
			_keysym = X11_BACK_SPACE;
			break;
		case KeyEvent.VK_TAB:
			_keysym = X11_TAB;
			break;
		case KeyEvent.VK_ENTER:
			_keysym = X11_ENTER;
			break;
		case KeyEvent.VK_ESCAPE:
			_keysym = X11_ESCAPE;
			break;
		case KeyEvent.VK_ALT:
			_keysym = X11_ALT;
			break;
		case KeyEvent.VK_ALT_GRAPH:
			_keysym = X11_ALT_GRAPH;
			break;
		case KeyEvent.VK_CONTROL:
			_keysym = X11_CONTROL;
			break;
		case KeyEvent.VK_SHIFT:
			_keysym = X11_SHIFT;
			break;
		case X11_WINDOWS:
			// WTF? Java is not giving us the X11_WINDOWS value when pressing
			// the windows key.
			// TODO: is this only on Linux?
			_keycode = KeyEvent.VK_WINDOWS;
			break;
		}
	}

	protected byte[] getKeyEvent() {
		byte[] events = new byte[0];
		if (_extra_preceding_events != null) {
			for (KeyboardEvent e : _extra_preceding_events) {
				events = Util.concat(events, e.getBytes());
			}
		}
		
		if(bypass_original_event){
			return events;
		}

		logger.debug(this);
		byte[] ev;
		if (session.extended_key_event) {
			ev = getExtendedKeyEvent();
		} else {
			ev = getSimpleKeyEvent();
		}

		return Util.concat(events, ev);
	}

	protected byte[] getExtendedKeyEvent() {
		int rfbcode = KeyboardEventMap.java2rfb(_keycode);
		byte[] buf = new byte[12];
		buf[0] = (byte) Encodings.QEMU;
		buf[1] = (byte) 0; // *submessage-type*
		buf[2] = (byte) 0; // downflag
		buf[3] = (byte) (_press ? 1 : 0); // downflag
		byte[] b = RfbUtil.toBytes(_keysym); // *keysym*
		buf[4] = b[0];
		buf[5] = b[1];
		buf[6] = b[2];
		buf[7] = b[3];
		b = RfbUtil.toBytes(rfbcode, b); // *keycode*
		buf[8] = b[0];
		buf[9] = b[1];
		buf[10] = b[2];
		buf[11] = b[3];
		return buf;
	}

	protected byte[] getSimpleKeyEvent() {
		byte[] buf = new byte[8];
		buf[0] = (byte) Encodings.KEYBOARD_EVENT;
		buf[1] = (byte) (_press ? 1 : 0);
		buf[2] = (byte) 0;
		buf[3] = (byte) 0;
		buf[4] = (byte) ((_keysym >> 24) & 0xff);
		buf[5] = (byte) ((_keysym >> 16) & 0xff);
		buf[6] = (byte) ((_keysym >> 8) & 0xff);
		buf[7] = (byte) (_keysym & 0xff);
		return buf;
	}
	
	protected void handleLinuxPecularities() throws KeyUndefinedException{
	}
	
	private void handleMacPecularities(KeyEvent evt){
		char keychar = (char) _keysym;

		// WTF? Mac Problems, VK_LESS is VK_BACK_QUOTE
		// fix for danish layout
		if (_keycode == KeyEvent.VK_BACK_QUOTE){

			// WTF? In snow leopard there is no OS event sent for the danish '<' button
			// when Ctrl-Alt is held down?
			// Make it possible to send with Alt key instead.
			if(evt.isAltDown()){
				bypass_original_event = true;
				addExtraEvent(_keysym,KeyEvent.VK_ALT_GRAPH, _press);
				addExtraEvent(_keysym,KeyEvent.VK_LESS, _press);
			}
			if(keychar == '<' || keychar == '>') {
				_keycode = KeyEvent.VK_LESS;	
			}
		}
	}
	
	private void handleWinPecularities(KeyEvent evt){
	}
	
	private void handleJavaPecularities(KeyEvent evt){
		// not every key release has a preceding key press!?!
		// keep track of key presses, and do press ourself if it wasn't 
		// triggered.
		if (_press) {
			session.keys_pressed.put(_keycode, _keysym);
		} else {
			if (!session.keys_pressed.containsKey(_keycode)) {
				// Do press ourself.
				logger.debug("Writing key pressed event for " + (char)_keysym
						+ " keycode: " + _keycode);
				addExtraEvent(_keysym, _keycode, true);
			} else {
				session.keys_pressed.remove(_keycode);
			}
		}		
	}

	private void handlePecularaties(KeyEvent evt) throws KeyUndefinedException{
		if (Util.isMac()) {
			handleMacPecularities(evt);
		}
		else if (Util.isWin()) {
			handleWinPecularities(evt);
		}
		else if(Util.isLinux()){
			handleLinuxPecularities();
		}
		
		handleJavaPecularities(evt);
	}

	public String toString(){
		return String.format("%s key event, keysym:%d keychar:'%s' keycode:%d(%s) %s", new Object[] {
			session.extended_key_event ? "extended" : "simple ",
			_keysym,
			(char) _keysym,
			_keycode,
			KeyEntry.keyCodeToString(_keycode),
			_press ? " press" : " release"
		});
	}

	protected List<KeyboardEvent> getAdditionalEvents() {
		return _extra_preceding_events;
	}
	
}
