package com.tigervnc.rfb;

public class RfbUtil {
	
	public static byte[] toBytes(int i, byte[] b) {
		b[0] = (byte) ((i >> 24) & 0xff);
		b[1] = (byte) ((i >> 16) & 0xff);
		b[2] = (byte) ((i >> 8) & 0xff);
		b[3] = (byte) (i & 0xff);
		return b;
	}

	public static byte[] toBytes(int i) {
		return toBytes(i, new byte[4]);
	}
}
