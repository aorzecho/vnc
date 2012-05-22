package com.tigervnc.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.tigervnc.log.VncLogger;

import com.tigervnc.Util;
import com.tigervnc.VncViewer;
import com.tigervnc.rfb.RfbProto;
import com.tigervnc.rfb.message.KeyEntry;
import com.tigervnc.rfb.message.KeyboardEvent;
import com.tigervnc.rfb.message.KeyboardEvent.KeyUndefinedException;
import com.tigervnc.rfb.message.KeyboardEventMap;
import java.awt.Component;
import java.util.*;
import static java.awt.event.KeyEvent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CanvasKeyListener implements KeyListener {

	private static VncLogger logger = VncLogger.getLogger(CanvasKeyListener.class);
	
	private static final int[] MODIFIER_KEYCODES = new int[] {VK_ALT, VK_ALT_GRAPH, VK_CONTROL, VK_SHIFT, VK_META};
	
	private final RfbProto rfb;
	private final VncCanvas canvas;
	private final VncViewer.Session session;
	
	private final AtomicBoolean firstKey = new AtomicBoolean(true);
	
	private Set <Integer> pressed = Collections.synchronizedSet(new HashSet <Integer>());
	
	public CanvasKeyListener(VncCanvas canvas){
		this.rfb = canvas.rfb;
		this.canvas = canvas;
		this.session = canvas.viewer.session;
	}
	
	private void process(KeyEvent evt) {
		if (rfb != null && rfb.inNormalProtocol) {
			if (!canvas.inputEnabled) {
				if ((evt.getKeyChar() == 'r' || evt.getKeyChar() == 'R')
						&& evt.getID() == KeyEvent.KEY_PRESSED) {
					// Request screen update.
					try {
						rfb.writeFramebufferUpdateRequest(0, 0,
								rfb.server.fb_width, rfb.server.fb_height,
								false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				// Input enabled.
				synchronized (rfb) {
					try {
						if (firstKey.compareAndSet(true, false) && session.extended_key_event) { // clean modifier keys state
							logger.info("First key event - sending modifier keys events to reset state");
							for (KeyEntry key : KeyEntry.MODIFIER_KEYS) {
								rfb.writeKeyboardEvent(key.keysym, key.keycode, true);
								rfb.writeKeyboardEvent(key.keysym, key.keycode, false);
							}
						}
						List<KeyboardEventMap.EvtEntry> remap = session.kbEvtMap.remapEvent(evt);
						if (remap != null) {
							for (KeyboardEventMap.EvtEntry mappedEvt : remap) {
								rfb.writeKeyboardEvent(
										mappedEvt.key.keysym,
										mappedEvt.key.keycode,
										mappedEvt.evtId == KeyEvent.KEY_PRESSED);
							}
						} else if (evt.getID() == KeyEvent.KEY_TYPED) {
							logger.debug("Ignored key typed event: " + evt);
						} else {
							rfb.writeKeyboardEvent(evt);
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (KeyUndefinedException e) {
						System.err.println(e.getMessage());
						// consume
					}
					rfb.notify();
				}
			}
		}
		// Don't ever pass keyboard events to AWT for default processing.
		// Otherwise, pressing Tab would switch focus to ButtonPanel etc.
		evt.consume();
	}

	private void updateModifersState (KeyEvent evt) throws Exception {
		for (int keycode : MODIFIER_KEYCODES)
			if (evt.getKeyCode() != keycode)
				switch (keycode) {
					case VK_ALT: updateMod(keycode, evt.isAltDown()); break;
					case VK_ALT_GRAPH: updateMod(keycode, evt.isAltGraphDown()); break;
					case VK_CONTROL: updateMod(keycode, evt.isControlDown()); break;
					case VK_SHIFT: updateMod(keycode, evt.isShiftDown()); break;
					case VK_META: updateMod(keycode, evt.isMetaDown()); break;
				}
			
	}
	
	private void updateMod (int keycode, boolean down) throws Exception {
		if (down ^ pressed.contains(keycode)) {
			rfb.writeKeyboardEvent(new KeyEvent(canvas, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, 0,
							0, keycode, KeyEvent.CHAR_UNDEFINED));
			if (down)
				pressed.add(keycode);
			else
				pressed.remove(keycode);
		}
	}
	
	//
	// Handle events.
	//
	@Override
	public void keyPressed(KeyEvent evt) {
		logger.debug(evt);
		if (evt.getKeyCode() != KeyEvent.VK_UNDEFINED)
			pressed.add(evt.getKeyCode());
		
		if (canvas.inputEnabled && rfb != null && rfb.inNormalProtocol) try {
			updateModifersState(evt);
			process(evt);
		} catch (Exception ex) {
			logger.error("Exception processing " + evt, ex);
		}
	}

	@Override
	public void keyReleased(KeyEvent evt) {
		logger.debug(evt);
		if (evt.getKeyCode() != KeyEvent.VK_UNDEFINED && !pressed.remove(evt.getKeyCode())) {
			//missing press event, process fake one
			logger.info("sending missing press event for " + evt);
			process(
				new KeyEvent(Component.class.cast(evt.getSource()), KeyEvent.KEY_PRESSED, evt.getWhen(), 
				evt.getModifiers(), evt.getKeyCode(), evt.getKeyChar(), evt.getKeyLocation())
				);
		}
		process(evt);
	}

	@Override
	// WTF? Java Windows doesn't get keypress / keyrelease for æøå!?!
	// the keycode is 0 (unknown!)
	// but on only keyTyped events.
	// we write them here!
	public void keyTyped(KeyEvent evt) {
		logger.debug(evt);
		process(evt);
	}
	
}
