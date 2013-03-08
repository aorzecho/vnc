package com.tigervnc.rfb.message;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.util.List;

import javax.swing.JFrame;

import junit.framework.Assert;

import org.junit.Test;

import com.tigervnc.Util;
import com.tigervnc.rfb.Encodings;
import com.tigervnc.rfb.message.KeyboardEvent.KeyUndefinedException;


public class KeyboardEventTest {

	private JFrame dummy = new JFrame();
	
	@Test
	public void test_getExtendedKeyEvent(){
		KeyboardEvent.extended_key_event = true;
		boolean down = true;
		byte[] key_ev = new KeyboardEvent((char)1,KeyEvent.VK_1,down).getBytes();		
		Assert.assertEquals(255, Encodings.QEMU);
//		    =============== ==================== ========== =======================
//			No. of bytes    Type                 [Value]    Description
//			=============== ==================== ========== =======================
//			1               ``U8``               255        *message-type*
//			1               ``U8``               0          *submessage-type*
//			2               ``U16``                         *down-flag*
//			4               ``U32``                         *keysym*
//			4               ``U32``                         *keycode*
//			=============== ==================== ========== ======================
				
		Assert.assertEquals((byte)Encodings.QEMU, key_ev[0]);
		Assert.assertEquals(0, key_ev[1]);
		// down-flag
		Assert.assertEquals(1, ByteBuffer.wrap(new byte[]{key_ev[2], key_ev[3]}).asShortBuffer().get());
		// keysym
		Assert.assertEquals(1, ByteBuffer.wrap(new byte[]{key_ev[4], key_ev[5], key_ev[6], key_ev[7]}).asIntBuffer().get());
		// keycode
		Assert.assertEquals(KeyboardEventMap.java2rfb[KeyEvent.VK_1], ByteBuffer.wrap(new byte[]{key_ev[8], key_ev[9], key_ev[10], key_ev[11]}).asIntBuffer().get());
	}
	
	@Test
	public void test_ctrl_and_alt_should_produce_alt_gr_event() throws KeyUndefinedException{
		KeyboardEvent.extended_key_event = true;
		int modifiers = KeyEvent.CTRL_DOWN_MASK;
		KeyboardEvent rfb_ke = new KeyboardEvent(new KeyEvent(dummy, KeyEvent.KEY_PRESSED, 0, modifiers, KeyEvent.VK_ALT));
		
		// ctrl + alt
		Assert.assertEquals(rfb_ke._extra_preceding_events.size(), 2);
		KeyboardEvent control_release_event = rfb_ke._extra_preceding_events.get(0);
		KeyboardEvent alt_gr_press_event = rfb_ke._extra_preceding_events.get(1);
		
		// should release control
		
		Assert.assertEquals(KeyEvent.VK_CONTROL, control_release_event._keycode);
		Assert.assertEquals(false, control_release_event._press);
		
		// should get alt gr press event
		Assert.assertEquals(KeyEvent.VK_ALT_GRAPH, alt_gr_press_event._keycode);
		Assert.assertEquals(true, alt_gr_press_event._press);
		
		KeyboardEvent._alt_gr_pressed = false; // reset
		
		// the other way around: alt + ctrl
		modifiers = KeyEvent.ALT_DOWN_MASK;
		rfb_ke = new KeyboardEvent(new KeyEvent(dummy, KeyEvent.KEY_PRESSED, 0, modifiers, KeyEvent.VK_CONTROL));
		
		Assert.assertEquals(rfb_ke._extra_preceding_events.size(), 2);
		KeyboardEvent alt_release_event = rfb_ke._extra_preceding_events.get(0);
		alt_gr_press_event = rfb_ke._extra_preceding_events.get(1);
		
		// should release alt
		Assert.assertEquals(KeyEvent.VK_ALT, alt_release_event._keycode);
		Assert.assertEquals(false, alt_release_event._press);
		
		// should get alt gr press event
		Assert.assertEquals(KeyEvent.VK_ALT_GRAPH, alt_gr_press_event._keycode);
		Assert.assertEquals(true, alt_gr_press_event._press);

	}
	
	@Test
	public void test_ctrl_and_alt_should_produce_alt_gr_release() throws KeyUndefinedException{
		// ctrl + alt release
		int modifiers = 0; // modifiers are released
		KeyboardEvent._alt_gr_pressed = true;
		KeyboardEvent rfb_ke = new KeyboardEvent(new KeyEvent(dummy, KeyEvent.KEY_RELEASED, 0, modifiers, KeyEvent.VK_ALT));
		KeyboardEvent alt_gr_release_event = rfb_ke._extra_preceding_events.get(0);
		
		// should release alt gr
		Assert.assertEquals(KeyEvent.VK_ALT_GRAPH, alt_gr_release_event._keycode);
		Assert.assertEquals(false, alt_gr_release_event._press);
		
		// the other way around: alt + ctrl
		KeyboardEvent._alt_gr_pressed = true;
		rfb_ke = new KeyboardEvent(new KeyEvent(dummy, KeyEvent.KEY_RELEASED, 0, modifiers, KeyEvent.VK_CONTROL));
		alt_gr_release_event = rfb_ke._extra_preceding_events.get(0);
		
		// should release alt
		Assert.assertEquals(KeyEvent.VK_ALT_GRAPH, alt_gr_release_event._keycode);
		Assert.assertEquals(false, alt_gr_release_event._press);
	}
	
	@Test
	public void test_æåø_on_danish_linux_keyboard_layout() throws KeyUndefinedException{
		KeyboardEvent e = new KeyboardEvent('æ', KeyEvent.VK_UNDEFINED, false);
		
		if(Util.isLinux()){
			e.handleLinuxPecularities();
			
			KeyboardEvent æ_press_event = e._extra_preceding_events.get(0);
			KeyboardEvent æ_release_event = e._extra_preceding_events.get(1);
			
			Assert.assertEquals(KeyEvent.VK_SEMICOLON, æ_press_event._keycode);
			Assert.assertEquals(KeyEvent.VK_SEMICOLON, æ_release_event._keycode);	
		}
	}
}
