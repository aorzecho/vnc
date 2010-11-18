package com.tigervnc.rfb.message;

import com.tigervnc.rfb.Encodings;

public class SetPixelFormat {
	
	private int bits_per_pixel;
	private int depth;
	private boolean big_endian;
	private boolean true_color;
	private int red_max;
	private int green_max;
	private int blue_max;
	private int red_shift;
	private int green_shift;
	private int blue_shift;
	
	public SetPixelFormat(int bits_per_pixel, int depth, boolean big_endian, boolean true_color, int red_max, int green_max, int blue_max, int red_shift, int green_shift, int blue_shift) {
		this.bits_per_pixel = bits_per_pixel;
		this.depth = depth;
		this.big_endian = big_endian;
		this.true_color = true_color;
		this.red_max = red_max;
		this.green_max = green_max;
		this.blue_max = blue_max;
		this.red_max = red_max;
		this.blue_shift = blue_shift;
		this.red_shift = red_shift;
		this.green_shift = green_shift;
	}
	
	public byte[] getBytes(){
		byte[] b = new byte[20];
		b[0] = (byte) Encodings.SET_PIXEL_FORMAT;
		b[4] = (byte) bits_per_pixel;
		b[5] = (byte) depth;
		b[6] = (byte) (big_endian ? 1 : 0);
		b[7] = (byte) (true_color ? 1 : 0);
		b[8] = (byte) ((red_max >> 8) & 0xff);
		b[9] = (byte) (red_max & 0xff);
		b[10] = (byte) ((green_max >> 8) & 0xff);
		b[11] = (byte) (green_max & 0xff);
		b[12] = (byte) ((blue_max >> 8) & 0xff);
		b[13] = (byte) (blue_max & 0xff);
		b[14] = (byte) red_shift;
		b[15] = (byte) green_shift;
		b[16] = (byte) blue_shift;
		return b;
	}
	

}
