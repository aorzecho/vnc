package com.tigervnc.rfb;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import junit.framework.Assert;

import org.junit.Test;

import com.tigervnc.rfb.message.KeyboardEvent;
import com.tigervnc.rfb.message.SetEncodings;



public class RfbProtoTest {

	@Test
	public void test_to_bytes(){
		int i = 1;
		byte[] b = RfbUtil.toBytes(i);
		Assert.assertEquals(0, b[0]);
		Assert.assertEquals(0, b[1]);
		Assert.assertEquals(0, b[2]);
		Assert.assertEquals(1, b[3]);
	}

}
