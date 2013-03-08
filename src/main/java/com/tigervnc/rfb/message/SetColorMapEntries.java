package com.tigervnc.rfb.message;

import com.tigervnc.rfb.Encodings;

public class SetColorMapEntries implements IServerMessage{

	private int first_color;
	private int colors;
	private int[] red;
	private int[] green;
	private int[] blue;
	
	public SetColorMapEntries(int first_color, int colors, int[] red, int[] green, int[] blue){
		this.first_color = first_color;
		this.colors = colors;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}
	
	public byte[] getBytes(){
		byte[] b = new byte[6 + colors * 6];

		b[0] = (byte) Encodings.SET_COLOR_MAP_ENTRIES;
		b[2] = (byte) ((first_color >> 8) & 0xff);
		b[3] = (byte) (first_color & 0xff);
		b[4] = (byte) ((colors >> 8) & 0xff);
		b[5] = (byte) (colors & 0xff);

		for (int i = 0; i < colors; i++) {
			b[6 + i * 6] = (byte) ((red[i] >> 8) & 0xff);
			b[6 + i * 6 + 1] = (byte) (red[i] & 0xff);
			b[6 + i * 6 + 2] = (byte) ((green[i] >> 8) & 0xff);
			b[6 + i * 6 + 3] = (byte) (green[i] & 0xff);
			b[6 + i * 6 + 4] = (byte) ((blue[i] >> 8) & 0xff);
			b[6 + i * 6 + 5] = (byte) (blue[i] & 0xff);
		}
		
		return b;
	}
}
