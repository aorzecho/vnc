package com.tigervnc.rfb.message;

import java.util.List;

import com.tigervnc.rfb.Encodings;

public class SetEncodings implements IServerMessage{

	List<Integer> encodings;
	
	public SetEncodings(List<Integer> encodings){
		this.encodings = encodings;
	}
	
	public byte[] getBytes(){
		int len = encodings.size();
		byte[] b = new byte[4 + 4 * len];
		b[0] = (byte) Encodings.SET_ENCODINGS;
		b[2] = (byte) ((len >> 8) & 0xff);
		b[3] = (byte) (len & 0xff); // *number-of-encodings*

		int i = 0;
		for(int enc : encodings){
			b[4 + 4 * i] = (byte) ((enc >> 24) & 0xff);
			b[5 + 4 * i] = (byte) ((enc >> 16) & 0xff);
			b[6 + 4 * i] = (byte) ((enc >> 8) & 0xff);
			b[7 + 4 * i] = (byte) (enc & 0xff);
			i++;
		}
		return b;		
	}
}
