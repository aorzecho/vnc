/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tigervnc.rfb.message;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author aorzecho
 */
public final class KeyEntry {

	public static Logger logger = Logger.getLogger(KeyEntry.class);
	public static final KeyEntry ALT = new KeyEntry(KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED, KeyEvent.ALT_DOWN_MASK);
	public static final KeyEntry ALT_GR = new KeyEntry(KeyEvent.VK_ALT_GRAPH, KeyEvent.CHAR_UNDEFINED, KeyEvent.ALT_GRAPH_DOWN_MASK);
	public static final KeyEntry CTRL = new KeyEntry(KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED, KeyEvent.CTRL_DOWN_MASK);
	public static final KeyEntry SHIFT = new KeyEntry(KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED, KeyEvent.SHIFT_DOWN_MASK);
	public static final KeyEntry META = new KeyEntry(KeyEvent.VK_META, KeyEvent.CHAR_UNDEFINED, KeyEvent.META_DOWN_MASK);
	public final int keycode;
	public final int keysym;
	public final boolean alt;
	public final boolean altGr;
	public final boolean ctrl;
	public final boolean shift;
	public final boolean meta;

	public KeyEntry(int keycode, int keysym, int modifiers) {
		this.keycode = keycode;
		this.keysym = keysym;
		alt = (modifiers & KeyEvent.ALT_DOWN_MASK) != 0;
		altGr = (modifiers & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0;
		ctrl = (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0;
		shift = (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0;
		meta = (modifiers & KeyEvent.META_DOWN_MASK) != 0;
	}

	public KeyEntry(KeyEvent e) {
		keycode = e.getKeyCode();
		keysym = e.getKeyChar();
		alt = e.isAltDown();
		altGr = e.isAltGraphDown();
		ctrl = e.isControlDown();
		shift = e.isShiftDown();
		meta = e.isMetaDown();
	}

	//
	public KeyEntry(String txt) {
		List<String> key = Arrays.asList(txt.trim().split("\\+"));
		String[] keyname = key.get(0).split("[|]");
		int _keycode = KeyEvent.VK_UNDEFINED;
		try {
			_keycode = KeyEvent.class.getField(keyname[0]).getInt(null);
		} catch (Exception ex) {
			logger.error("Unable to find keycode for " + keyname, ex);
		}
		keycode = _keycode;
		int _keysym = KeyEvent.CHAR_UNDEFINED;
		if (keyname.length == 2) {
			try {
				_keysym = Integer.valueOf(keyname[1]);
			} catch (Exception ex) {
				logger.error("Unable to find keycode for " + keyname, ex);
			}
		}
		keysym = _keysym;
		alt = key.contains("ALT");
		altGr = key.contains("ALT_GRAPH");
		ctrl = key.contains("CTRL");
		shift = key.contains("SHIFT");
		meta = key.contains("META");
	}

	// InputEvent compatibile mask
	public int getModifierMask() {
		int mask = 0;
		mask |= alt ? InputEvent.ALT_DOWN_MASK : 0;
		mask |= altGr ? InputEvent.ALT_GRAPH_DOWN_MASK : 0;
		mask |= ctrl ? InputEvent.CTRL_DOWN_MASK : 0;
		mask |= shift ? InputEvent.SHIFT_DOWN_MASK : 0;
		mask |= meta ? InputEvent.META_DOWN_MASK : 0;
		return mask;
	}

	public KeyboardEventMap.EvtEntry[] getExtendedWriteEvents() {
		Deque<KeyboardEventMap.EvtEntry> evts = new ArrayDeque<KeyboardEventMap.EvtEntry>();
		evts.add(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_PRESSED, this));
		evts.add(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_RELEASED, this));
		if (alt) {
			evts.addFirst(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_PRESSED, ALT));
			evts.addLast(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_RELEASED, ALT));
		}
		if (altGr) {
			evts.addFirst(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_PRESSED, ALT_GR));
			evts.addLast(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_RELEASED, ALT_GR));
		}
		if (ctrl) {
			evts.addFirst(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_PRESSED, CTRL));
			evts.addLast(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_RELEASED, CTRL));
		}
		if (shift) {
			evts.addFirst(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_PRESSED, SHIFT));
			evts.addLast(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_RELEASED, SHIFT));
		}
		if (meta) {
			evts.addFirst(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_PRESSED, META));
			evts.addLast(new KeyboardEventMap.EvtEntry(KeyEvent.KEY_RELEASED, META));
		}
		return evts.toArray(new KeyboardEventMap.EvtEntry[evts.size()]);
	}

	@Override
	public String toString() {
		return String.format("%s(x%h/%d)|%d(x%h/%s)%s%s%s%s%s", new Object[] {
			keyCodeToString(keycode),
			keycode,
			keycode,
			keysym,
			keysym,
			(char)keysym,
			alt ?	"+ALT" : "",
			altGr ?	"+ALT_GRAPH" : "",
			ctrl ?	"+CTRL" : "",
			shift ?	"+SHIFT" : "",
			meta ?	"+META" : ""
			
		});
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof KeyEntry)) {
			return false;
		}
		KeyEntry other = KeyEntry.class.cast(obj);
		return other.alt == this.alt
				&& other.altGr == this.altGr
				&& other.ctrl == this.ctrl
				&& other.shift == this.shift
				&& other.meta == this.meta
				&& other.keycode == this.keycode
				&& other.keysym == this.keysym;
	}

	@Override
	public int hashCode() {
		return (17 * getModifierMask()) ^ (keycode * 33) ^ keysym;
	}

	
	
	private static final Map<Integer, String> keycode2name = new HashMap<Integer, String>();
	
	static {
		for (Field f : KeyEvent.class.getFields()) {
			try {
				if (f.getName().startsWith("VK_")) {
					keycode2name.put(f.getInt(null), f.getName());
				}
			} catch (IllegalArgumentException ex) {
				logger.error(ex);
			} catch (IllegalAccessException ex) {
				logger.error(ex);
			}
		}
	}
	
	public static String keyCodeToString(int keycode) {
		return keycode2name.containsKey(keycode) 
				? keycode2name.get(keycode)
				: "UNKNOWN";
				
	}
}
