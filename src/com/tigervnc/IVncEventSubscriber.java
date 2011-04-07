package com.tigervnc;

public interface IVncEventSubscriber {

	public void init(String s);
	
	public void connectionError(String s, Exception e);
	
	public void destroy(String s);
}
