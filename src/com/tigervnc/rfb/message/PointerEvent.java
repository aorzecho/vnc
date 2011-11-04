package com.tigervnc.rfb.message;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.tigervnc.Util;
import com.tigervnc.rfb.Encodings;

public class PointerEvent implements IServerMessage {

	/*
	 * ... the current state of buttons 1 to 8 are represented by bits 0 to 7 of
	 * button-mask respectively, 0 meaning up, 1 meaning down (pressed). On a
	 * conventional mouse, buttons 1, 2 and 3 correspond to the left, middle and
	 * right buttons on the mouse. On a wheel mouse, each step of the wheel
	 * upwards is represented by a press and release of button 4, and each step
	 * downwards is represented by a press and release of button 5.
	 */

	// RFB MASKS
	public static final int RELEASE_ALL = 0;

	public static final int BUTTON1_MASK = 1;
	public static final int BUTTON2_MASK = (1 << 1);
	public static final int BUTTON3_MASK = (1 << 2);

	public static final int SCROLL_UP_MASK = (1 << 3);
	public static final int SCROLL_DOWN_MASK = (1 << 4);

	private MouseEvent evt;
	private static int buttonMask = 0;

	public PointerEvent(MouseEvent e) {
		evt = e;
	}

	protected int getButtonMask(int evtId) {

		int mods = evt.getModifiers();

		switch (evtId) {
		case MouseEvent.MOUSE_PRESSED:
			if (isButton2Press(mods)) {
				buttonMask = BUTTON2_MASK;
			} else if (isButton3Press(mods)) {
				buttonMask = BUTTON3_MASK;
			} else {
				// Note: For some reason, AWT does not set BUTTON1_MASK on left
				// button presses. Here we think that it was the left button if
				// modifiers do not include BUTTON2_MASK or BUTTON3_MASK
				buttonMask = BUTTON1_MASK;
			}
			break;
		case MouseEvent.MOUSE_RELEASED:
			// 0 for all buttons
			buttonMask = RELEASE_ALL;
			break;
		case MouseEvent.MOUSE_WHEEL:
			if (isScrollUp()) {
				buttonMask = SCROLL_UP_MASK;
			} else {
				buttonMask = SCROLL_DOWN_MASK;
			}
			break;
		}

		return buttonMask;
	}

	private boolean isScrollUp() {
		if (evt instanceof MouseWheelEvent) {
			return ((MouseWheelEvent) evt).getWheelRotation() < 0 ? true : false;
		}
		return false;
	}

	private boolean isScroll() {
		return evt.getID() == MouseEvent.MOUSE_WHEEL;
	}

	private boolean isButton3Press(int modifiers) {
		return (modifiers & InputEvent.BUTTON3_MASK) != 0;
	}

	protected boolean isButton2Press(int modifiers) {
		return (modifiers & InputEvent.BUTTON2_MASK) != 0;
	}

	protected byte[] getPointerEvent(int buttonMask) {
		byte[] buf = new byte[6];
		int x = evt.getX();
		int y = evt.getY();
		buf[0] = (byte) Encodings.POINTER_EVENT;
		buf[1] = (byte) buttonMask;
		buf[2] = (byte) ((x >> 8) & 0xff);
		buf[3] = (byte) (x & 0xff);
		buf[4] = (byte) ((y >> 8) & 0xff);
		buf[5] = (byte) (y & 0xff);
		return buf;
	}

	protected byte[] getPointerEvent() {
		int mask = getButtonMask(evt.getID());
		return getPointerEvent(mask);
	}

	protected byte[] getScrollEvent() {
		byte[] buf = getPointerEvent();
		int mask = getButtonMask(MouseEvent.MOUSE_RELEASED);
		buf = Util.concat(buf, getPointerEvent(mask));
		return buf;
	}

	@Override
	public byte[] getBytes() {
		if (isScroll()) {
			return getScrollEvent();
		} else {
			return getPointerEvent();
		}
	}
	// if (evtId == MouseEvent.MOUSE_PRESSED) {
	// if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
	// pointerMask = mask2;
	// modifiers &= ~ALT_MASK;
	// } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
	// pointerMask = mask3;
	// modifiers &= ~META_MASK;
	// } else {
	// pointerMask = 1;
	// }
	// } else if (evtId == MouseEvent.MOUSE_RELEASED) {
	// pointerMask = 0;
	// if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
	// modifiers &= ~ALT_MASK;
	// } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
	// modifiers &= ~META_MASK;
	// }
	// }
}
