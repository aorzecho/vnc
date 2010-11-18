package com.tigervnc.rfb.message;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import com.tigervnc.rfb.message.Version;

public class VersionTest {

	@Test
	public void testNewVersion(){
		Version version = new Version(3,8);
		System.out.println(version.toString());
		Assert.assertEquals("RFB 003.008\n", version.toString());
	}
	
	@Test
	public void testGetMajor() throws IOException {
		Version version = new Version(new DataInputStream(new ByteArrayInputStream("RFB 003.008\n".getBytes())));
		Assert.assertEquals(3, version.getMajor());
		Assert.assertEquals(8, version.getMinor());
	}

}
