package com.tigervnc.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.tigervnc.Util;
import com.tigervnc.rfb.RfbProto;
import com.tigervnc.rfb.message.KeyEntry;
import com.tigervnc.rfb.message.KeyboardEvent;
import com.tigervnc.rfb.message.KeyboardEvent.KeyUndefinedException;
import com.tigervnc.rfb.message.KeyboardEventMap;
import java.util.*;
import static java.awt.event.KeyEvent.*;

public class CanvasKeyListener implements KeyListener {

	private static Logger logger = Logger.getLogger(CanvasKeyListener.class);
	
	private RfbProto rfb;
	private VncCanvas canvas;
	
	private Set <Integer> pressed = Collections.synchronizedSet(new HashSet <Integer>());
	
	public CanvasKeyListener(VncCanvas canvas){
		this.rfb = canvas.rfb;
		this.canvas = canvas;
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
						List<KeyboardEventMap.EvtEntry> remap = KeyboardEventMap.getInstance().remapEvent(evt);
						if (remap != null) {
							for (KeyboardEventMap.EvtEntry mappedEvt : remap)
								rfb.writeKeyboardEvent(
									mappedEvt.key.keysym, 
									mappedEvt.key.keycode, 
									mappedEvt.evtId == KeyEvent.KEY_PRESSED);
						} else if (evt.getID() == KeyEvent.KEY_TYPED) {
                                                    logger.debug("Ignored key typed event: " + evt );
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
		for (int keycode : new int[] {VK_ALT, VK_ALT_GRAPH, VK_CONTROL, VK_SHIFT, VK_META})
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
			rfb.writeKeyboardEvent((int) KeyEvent.CHAR_UNDEFINED, keycode, down);
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
			logger.log(null, ex);
		}
	}

	@Override
	public void keyReleased(KeyEvent evt) {
		logger.debug(evt);
		if (evt.getKeyCode() != KeyEvent.VK_UNDEFINED && !pressed.remove(evt.getKeyCode())) {
			//missing press event, process fake one
			logger.info("sending missing press event for " + evt);
			process(
				new KeyEvent(evt.getComponent(), KeyEvent.KEY_PRESSED, evt.getWhen(), 
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
