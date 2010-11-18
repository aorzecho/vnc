package com.tigervnc.rfb.message;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.junit.Test;

import com.tigervnc.rfb.Encodings;
import com.tigervnc.rfb.KeyMap;


public class KeyboardEventTest {

	@Test
	public void test_getExtendedKeyEvent(){
		KeyboardEvent.extended_key_event = true;
		boolean down = true;
		byte[] key_ev = new KeyboardEvent(1,KeyEvent.VK_1,down).getBytes();		
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
		Assert.assertEquals(KeyMap.java2rfb[KeyEvent.VK_1], ByteBuffer.wrap(new byte[]{key_ev[8], key_ev[9], key_ev[10], key_ev[11]}).asIntBuffer().get());
	}
}
