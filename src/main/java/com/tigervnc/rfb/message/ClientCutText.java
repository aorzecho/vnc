package com.tigervnc.rfb.message;

import com.tigervnc.rfb.Encodings;

public class ClientCutText implements IServerMessage {

	private String text;
	
	public ClientCutText(String text){
		this.text = text;
	}
	
	public byte[] getBytes(){
		byte[] b = new byte[8 + text.length()];

		b[0] = (byte) Encodings.CLIENT_CUT_TEXT;
		b[4] = (byte) ((text.length() >> 24) & 0xff);
		b[5] = (byte) ((text.length() >> 16) & 0xff);
		b[6] = (byte) ((text.length() >> 8) & 0xff);
		b[7] = (byte) (text.length() & 0xff);
		
		System.arraycopy(text.getBytes(), 0, b, 8, text.length());
		return b;
	}
}
