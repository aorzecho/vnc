package com.tigervnc;

public class VncEventSubscriber implements IVncEventSubscriber {

	@Override
	public void init(String s){}
	
	@Override
	public void connectionError(String s, Exception e){}
	
	@Override
	public void destroy(String s){}
	
	@Override
	public void updSetup(String s){}
	
	@Override
	public void fatalError(String s, Exception e){}
	
}
