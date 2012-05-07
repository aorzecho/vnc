package com.tigervnc;

interface IVncEventSubscriber {
	
	void init(String s);
	
	void connectionError(String s, Exception e);
	
	void destroy(String s);
	
	void updSetup(String s);
	
	void fatalError(String s, Exception e);
	
}
