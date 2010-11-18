package com.tigervnc.rfb.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version extends ClientMessage {

	private int major;
	private int minor;
	private Pattern version_pattern = Pattern.compile("RFB ([0-9]{3})\\.([0-9]{3})\n");
	
	public Version(int major, int minor){
		this.major = major;
		this.minor = minor;
	}
	
	public Version(DataInputStream is) throws IOException{
		super(is);
		
		Matcher matcher = version_pattern.matcher(new String(msg));
		if(!matcher.matches()){
			System.out.println("didnt match");
		}
		this.major = Integer.parseInt(matcher.group(1).replaceFirst("0*", ""));
		this.minor = Integer.parseInt(matcher.group(2).replaceFirst("0*", ""));
	}
	
	public int getMajor(){
		return major;
	}
	
	public int getMinor(){
		return minor;
	}
	
	public byte[] getBytes(){
		return toString().getBytes();
	}
	
	public int length(){
		return 12;
	}
	
	public String toString(){
		Formatter formatter = new Formatter();
		formatter.format("RFB %03d.%03d\n", major, minor);
		return formatter.toString();
	}
}
