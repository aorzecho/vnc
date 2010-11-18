package com.tigervnc.vncviewer;

public class Util {

	private static final String os = System.getProperty("os.name").toLowerCase();

	public static boolean isWin() {
		return os.indexOf("win") != -1 ? true : false;
	}

	public static boolean isMac() {
		return os.indexOf("mac") != -1 ? true : false;
	}
}
