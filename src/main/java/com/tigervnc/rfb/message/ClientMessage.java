package com.tigervnc.rfb.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Message to the client from the server.
 */
public abstract class ClientMessage implements IClientMessage {

	protected byte[] msg;
	
	public ClientMessage(DataInputStream is) throws IOException {
		msg = new byte[length()];
		is.read(msg, 0, msg.length);
	}
	
	public ClientMessage(){}
	
}
