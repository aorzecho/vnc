package com.tigervnc.rfb.message;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tigervnc.rfb.Encodings;
import com.tigervnc.rfb.KeyMap;
import com.tigervnc.rfb.RfbUtil;
import com.tigervnc.vncviewer.Util;

public class KeyboardEvent implements IServerMessage {

	public class KeyUndefinedException extends Exception {
	}

	// Set from the main vnc response loop VncViewer, refactor that...
	public static boolean extended_key_event = false;

	private static boolean alt_gr = false;

	/** Maps from keycode to keysym for all currently pressed keys. */
	private static Map<Integer, Integer> keys_pressed = new HashMap<Integer, Integer>();

	/** Get all pressed keys as a map from keycode to keysym */
	public static Map<Integer, Integer> getPressedKeys() throws IOException {
		return keys_pressed;
	}

	/** Clear all pressed keys */
	public static void clearPressedKeys() {
		keys_pressed.clear();
	}

	private int keysym;
	private int keycode;
	private boolean press;
	private List<KeyboardEvent> additional; 
	private boolean bypass_original_event = false;

	public KeyboardEvent(KeyEvent evt) throws KeyUndefinedException {
		keycode = evt.getKeyCode();
		keysym = evt.getKeyChar();
		press = (evt.getID() == KeyEvent.KEY_PRESSED);

		if (keycode == KeyEvent.VK_UNDEFINED) {
			throw new KeyUndefinedException();
		}
		handleShortcuts(evt);
		handlePecularaties(evt);
		keysym2x11();
	}

	public KeyboardEvent(int keysym, int keycode, boolean down) {
		this.keysym = keysym;
		this.keycode = keycode;
		this.press = down;
		keysym2x11();
	}

	/**
	 * Handles shortcuts in the client.
	 * 
	 * Ctrl-Alt-Delete = Ctrl-Alt-BackSpace
	 * 
	 * @param evt
	 * @return whether a shortcut was applied.
	 */
	private void handleShortcuts(KeyEvent evt) {		
		switch (keycode) {
		case KeyEvent.VK_BACK_SPACE:
			if (!(evt.isAltDown() && evt.isControlDown())) {
				return;
			}
			addExtraEvent(new KeyboardEvent(0xffe3, KeyEvent.VK_CONTROL, press));
			addExtraEvent(new KeyboardEvent(0xffe9, KeyEvent.VK_ALT, press));
			addExtraEvent(new KeyboardEvent(0xffff, KeyEvent.VK_DELETE, press));
			break;
		case KeyEvent.VK_META: 		
			// No Win key on Mac use META (cmd)
			keycode = KeyEvent.VK_WINDOWS;
			break;
		}
	}

	private void addExtraEvent(KeyboardEvent evt) {
		if (additional == null) {
			// max two additional at the moment
			additional = new LinkedList<KeyboardEvent>();
		}
		additional.add(evt);
	}
	
	public byte[] getBytes() {
		return getKeyEvent();
	}

	protected void keysym2x11() {
		switch (keycode) {
		case KeyEvent.VK_BACK_SPACE:
			keysym = 0xff08;
			break;
		case KeyEvent.VK_TAB:
			keysym = 0xff09;
			break;
		case KeyEvent.VK_ENTER:
			keysym = 0xff0d;
			break;
		case KeyEvent.VK_ESCAPE:
			keysym = 0xff1b;
			break;
		case KeyEvent.VK_ALT:
			keysym = 0xffe9;
			break;
		case KeyEvent.VK_ALT_GRAPH:
			keysym = 0xff7e;
			break;
		case KeyEvent.VK_CONTROL:
			keysym = 0xffe3;
			break;
		case KeyEvent.VK_SHIFT:
			keysym = 0xffe1;
			break;
		case KeyEvent.VK_META:
			keysym = 0xffe7;
			break;
		}
	}

	protected byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	protected byte[] getKeyEvent() {
		byte[] events = new byte[0];
		if (additional != null) {
			for (KeyboardEvent e : additional) {
				System.out.println(e);
				events = concat(events, e.getBytes());
			}
		}
		
		if(bypass_original_event){
			return events;
		}

		System.out.println(this);
		byte[] ev;
		if (extended_key_event) {
			ev = getExtendedKeyEvent();
		} else {
			ev = getSimpleKeyEvent();
		}

		return concat(events, ev);
	}

	protected byte[] getExtendedKeyEvent() {
		int rfbcode = KeyMap.java2rfb[keycode];
		byte[] buf = new byte[12];
		buf[0] = (byte) Encodings.QEMU;
		buf[1] = (byte) 0; // *submessage-type*
		buf[2] = (byte) 0; // downflag
		buf[3] = (byte) (press ? 1 : 0); // downflag
		byte[] b = RfbUtil.toBytes(keysym); // *keysym*
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
		buf[1] = (byte) (press ? 1 : 0);
		buf[2] = (byte) 0;
		buf[3] = (byte) 0;
		buf[4] = (byte) ((keysym >> 24) & 0xff);
		buf[5] = (byte) ((keysym >> 16) & 0xff);
		buf[6] = (byte) ((keysym >> 8) & 0xff);
		buf[7] = (byte) (keysym & 0xff);
		return buf;
	}
	
	private void handleMacPecularities(KeyEvent evt){
		char keychar = (char) keysym;

		// WTF? Mac Problems, VK_LESS is VK_BACK_QUOTE
		// fix for danish layout
		if (keycode == KeyEvent.VK_BACK_QUOTE){

			// WTF? In snow leopard there is no OS event sent for the danish '<' button
			// when Ctrl-Alt is held down?
			// Make it possible to send with Alt key instead.
			if(evt.isAltDown()){
				bypass_original_event = true;
				addExtraEvent(new KeyboardEvent(keysym,KeyEvent.VK_ALT_GRAPH, press));
				addExtraEvent(new KeyboardEvent(keysym,KeyEvent.VK_LESS, press));
			}
			if(keychar == '<' || keychar == '>') {
				keycode = KeyEvent.VK_LESS;	
			}
		}
	}
	
	private void handleWinPecularities(KeyEvent evt){
		// WTF? When danish layout VK_EQUALS is changed to DEAD_ACUTE
		if (keycode == KeyEvent.VK_DEAD_ACUTE) {
			keycode = KeyEvent.VK_EQUALS;
		}
		// WTF? no VK alt Gr on Windows, instead Ctrl + Alt
		else if (keycode == KeyEvent.VK_ALT) {
			if (evt.isControlDown()) {
				// press
				alt_gr = true;
				// release the by windows pressed control key
				addExtraEvent(new KeyboardEvent(keysym,
						KeyEvent.VK_CONTROL, false));
				keycode = KeyEvent.VK_ALT_GRAPH;
			} else if (alt_gr) {
				// release
				keycode = KeyEvent.VK_ALT_GRAPH;
				alt_gr = false;
			}
		}
	}
	
	private void handleJavaPecularities(KeyEvent evt){
		// not every key release has a preceding key press!?!
		// keep track of key presses, and do press ourself if it wasn't 
		// triggered.
		if (press) {
			keys_pressed.put(keycode, keysym);
		} else {
			if (!keys_pressed.containsKey(keycode)) {
				// Do press ourself.
				System.out.println("Writing key pressed event for " + keysym
						+ " keycode: " + keycode);
				addExtraEvent(new KeyboardEvent(keysym, keycode, true));
			} else {
				keys_pressed.remove(keycode);
			}
		}		
	}

	private void handlePecularaties(KeyEvent evt) {
		if (Util.isMac()) {
			handleMacPecularities(evt);
		}
		else if (Util.isWin()) {
			handleWinPecularities(evt);
		}
		
		handleJavaPecularities(evt);
	}
	
	public String toString(){
		return (extended_key_event ? "extended" : "simple ")
				 + "key event, keysym: " + keysym + " keychar: '" + (char)keysym + "'"
				 + " keycode: " + keycode
				 + (press ? " press" : " release");
	}
}
