package com.tigervnc.rfb.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;


public class AuthenticationTest {

	@Test
	public void test_authentication() throws IOException{
		String challenge = "challengechaleng";
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(challenge.getBytes()));
		Authentication auth = new Authentication(is, "password");
		Assert.assertEquals(challenge, auth.getChallenge());
		
	}
}
