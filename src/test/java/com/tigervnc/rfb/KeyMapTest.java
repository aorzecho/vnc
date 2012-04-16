package com.tigervnc.rfb;

import java.awt.event.KeyEvent;

import junit.framework.Assert;

import org.junit.Test;

import com.tigervnc.rfb.message.KeyboardEventMap;



public class KeyMapTest {

	@Test
	public void test_java2rfb(){
		Assert.assertEquals(0x1e, KeyboardEventMap.java2rfb(KeyEvent.VK_A).intValue());
		Assert.assertEquals(0xcd, KeyboardEventMap.java2rfb(KeyEvent.VK_RIGHT).intValue());
	}
}
