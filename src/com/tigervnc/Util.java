package com.tigervnc;

public class Util {

	private static final String os = System.getProperty("os.name").toLowerCase();

	public static boolean isWin() {
		return os.indexOf("win") != -1 ? true : false;
	}

	public static boolean isMac() {
		return os.indexOf("mac") != -1 ? true : false;
	}
	
	public static boolean isLinux(){
		return os.indexOf("linux") != -1 ? true : false;
	}
	
	public static byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
	
}
