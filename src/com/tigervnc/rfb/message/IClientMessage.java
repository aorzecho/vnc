package com.tigervnc.rfb.message;

public interface IClientMessage {

	public int length();
	
	public byte[] getBytes();
}
