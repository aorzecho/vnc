package com.tigervnc.rfb.message;

import com.tigervnc.rfb.Encodings;

public class FramebufferUpdateRequest implements IServerMessage{

	private int x;
	private int y;
	private int width;
	private int heigth;
	private boolean incremental;
	
	public FramebufferUpdateRequest(boolean incremental, int x_position, int y_position, int width, int height){
		this.x = x_position;
		this.y = y_position;
		this.width = width;
		this.heigth = height;
		this.incremental = incremental;
	}
	
	public byte[] getBytes() {
		byte[] b = new byte[10];
		b[0] = (byte) Encodings.FRAME_BUFFER_UPDATE_REQUEST; // *message-type*
		b[1] = (byte) (this.incremental ? 1 : 0);
		b[2] = (byte) ((x >> 8) & 0xff); // *x-position*
		b[3] = (byte) (x & 0xff);
		b[4] = (byte) ((y >> 8) & 0xff); // *y-position*
		b[5] = (byte) (y & 0xff);
		b[6] = (byte) ((width >> 8) & 0xff); // *width*
		b[7] = (byte) (width & 0xff);
		b[8] = (byte) ((heigth >> 8) & 0xff); // *height*
		b[9] = (byte) (heigth & 0xff);
		return b;
	}
	
}
