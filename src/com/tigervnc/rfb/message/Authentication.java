package com.tigervnc.rfb.message;

import java.io.DataInputStream;
import java.io.IOException;

import com.tigervnc.vncviewer.DesCipher;

public class Authentication extends ClientMessage {

	private String pwd;
	private byte[] challenge;
	
	public Authentication(DataInputStream is, String pwd) throws IOException{
		super(is);
		this.pwd = (pwd.length() > 8) ? pwd.substring(0, 8) : pwd;
		this.challenge = msg;
	}
	
	public byte[] getBytes() {
		byte[] key = { 0, 0, 0, 0, 0, 0, 0, 0 };
		System.arraycopy(pwd.getBytes(), 0, key, 0, pwd.length());
		DesCipher des = new DesCipher(key);

		byte[] result = challenge.clone();
		des.encrypt(result, 0, result, 0);
		des.encrypt(result, 8, result, 8);
		
		return result;
	}
	
	public int length(){
		return 16;
	}
	
	protected String getChallenge(){
		return new String(challenge);
	}

}
