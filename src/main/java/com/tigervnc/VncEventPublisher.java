package com.tigervnc;

import java.util.ArrayList;
import java.util.List;

import com.tigervnc.log.VncLogger;

public class VncEventPublisher {

	static VncLogger logger = VncLogger.getLogger(VncEventPublisher.class);
	
	private List<VncEventSubscriber> subscribers = new ArrayList<VncEventSubscriber>();
	
	public void subscribe(VncEventSubscriber listener){
		subscribers.add(listener);
	}
	
	public void publish(VncEvent e, String txt){
		publish(e, txt, null);
	}
	
	public void publish(VncEvent e, String txt, Exception ex){
		logger.info("publish: " + e + " " + txt + (ex != null ? " " + ex : ""));
		
		switch(e){
		case INIT:
			for(VncEventSubscriber s : subscribers){s.init(txt);}
			break;
		case CONNECTION_ERROR:
			for(VncEventSubscriber s : subscribers){s.connectionError(txt, ex);}
			break;
		case FATAL_ERROR:
			for(VncEventSubscriber s : subscribers){s.fatalError(txt, ex);}
			break;
		case DESTROY:
			for(VncEventSubscriber s : subscribers){s.destroy(txt);}
			break;			
		case UPD_SETUP:
			for(VncEventSubscriber s : subscribers){s.updSetup(txt);}
			break;			
		default:
			System.err.println("WTF?");
		}	
	}
}
