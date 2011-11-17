package com.tigervnc.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.tigervnc.Util;
import com.tigervnc.rfb.RfbProto;
import com.tigervnc.rfb.message.KeyboardEvent;
import com.tigervnc.rfb.message.KeyboardEvent.KeyUndefinedException;

public class CanvasKeyListener implements KeyListener {

	private RfbProto rfb;
	private VncCanvas canvas;
	
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
						rfb.writeKeyboardEvent(evt);
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


	//
	// Handle events.
	//
	@Override
	public void keyPressed(KeyEvent evt) {
		System.out.println("keyPressed char:" + evt.getKeyChar() + " code" + evt.getKeyCode());
		process(evt);
	}

	@Override
	public void keyReleased(KeyEvent evt) {
		System.out.println("keyReleased char:" + evt.getKeyChar() + " code" + evt.getKeyCode());
		process(evt);
	}

	@Override
	// WTF? Java Windows doesn't get keypress / keyrelease for æøå!?!
	// the keycode is 0 (unknown!)
	// but on only keyTyped events.
	// we write them here!
	public void keyTyped(KeyEvent evt) {
		System.out.println("keyTyped char:" + evt.getKeyChar() + " code" + evt.getKeyCode());
		
		if (Util.isWin()) {
			char keychar = evt.getKeyChar();
			try {
				if (KeyboardEvent.char2vk.containsKey(keychar)) {
					int vk = KeyboardEvent.char2vk.get(keychar);
					rfb.writeKeyboardEvent(keychar, vk, true);
					rfb.writeKeyboardEvent(keychar, vk, false);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		evt.consume();
	}

}
