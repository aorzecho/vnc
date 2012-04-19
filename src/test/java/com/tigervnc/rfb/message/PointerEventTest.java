package com.tigervnc.rfb.message;

import java.awt.Canvas;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;


import junit.framework.Assert;

import org.junit.Test;

public class PointerEventTest {

	private Canvas dummy = new Canvas();
	private int x = 0;
	private int y = 0;
	private int when = 0;
	private int id = 0;
	
	@Test
	public void test_get_scroll_event() {
		int modifiers = 0;
		modifiers = modifiers | MouseEvent.MOUSE_WHEEL;
		MouseWheelEvent evt = new MouseWheelEvent(dummy, MouseEvent.MOUSE_WHEEL, when, modifiers, x, y, 0, false, MouseEvent.MOUSE_WHEEL, 3, 3);
		byte[] ev = new PointerEvent(evt).getBytes();
		Assert.assertEquals(12, ev.length);
		Assert.assertEquals(5, ev[0]);
		Assert.assertEquals(PointerEvent.SCROLL_DOWN_MASK, ev[1]);
		Assert.assertEquals(0, ev[2]);
		Assert.assertEquals(0, ev[3]);
		Assert.assertEquals(0, ev[4]);
		Assert.assertEquals(0, ev[5]);
		
		Assert.assertEquals(5, ev[6]);
		Assert.assertEquals(PointerEvent.RELEASE_ALL, ev[7]);
	}
}
