package com.tigervnc.rfb;

import java.awt.event.KeyEvent;

import junit.framework.Assert;

import org.junit.Test;



public class KeyMapTest {

	@Test
	public void test_java2rfb(){
		Assert.assertEquals(0x1e, KeyMap.java2rfb[KeyEvent.VK_A]);
		Assert.assertEquals(0xcd, KeyMap.java2rfb[KeyEvent.VK_RIGHT]);
	}
}
