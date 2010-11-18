package com.tigervnc.rfb.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.tigervnc.rfb.CapabilityInfo;
import com.tigervnc.rfb.CapsContainer;
import com.tigervnc.rfb.RfbUtil;

public class ServerInit {

	public int fb_width;
	public int fb_height;
	public int bits_per_pixel;
	public int depth;
	public boolean big_endian;
	public boolean true_color;
	public int red_max;
	public int green_max;
	public int blue_max;
	public int blue_shift;
	public int green_shift;
	public int red_shift;
	public String name;
	
	public CapsContainer serverCaps = CapsContainer.getServerCapabilities();
	public CapsContainer clientCaps = CapsContainer.getClientCapabilities();
	public CapsContainer encodingTypes = CapsContainer.getEncodingCapabilities();
	
	public ServerInit(DataInputStream is, boolean tight_vnc_proto) throws IOException{
		fb_width = is.readUnsignedShort();
		fb_height = is.readUnsignedShort();
		bits_per_pixel = is.readUnsignedByte();
		depth = is.readUnsignedByte();
		big_endian = (is.readUnsignedByte() != 0);
		true_color = (is.readUnsignedByte() != 0);
		red_max = is.readUnsignedShort();
		green_max = is.readUnsignedShort();
		blue_max = is.readUnsignedShort();
		red_shift = is.readUnsignedByte();
		green_shift = is.readUnsignedByte();
		blue_shift = is.readUnsignedByte();
		is.read(new byte[3]);
		int name_length = is.readInt();
		byte[] name = new byte[name_length];
		is.read(name, 0, name_length);
		this.name = new String(name);
		
		if(tight_vnc_proto){
			int server_message_types = is.readUnsignedShort();
			int client_message_types = is.readUnsignedShort();
			int encoding_types = is.readUnsignedShort();
			is.read(new byte[2]);
			readCapabilityList(serverCaps, server_message_types, is);
			readCapabilityList(clientCaps, client_message_types, is);
			readCapabilityList(encodingTypes, encoding_types, is);
		}
	}
	
	public int length(){
		return 24;
	}
	
	private void readCapabilityList(CapsContainer caps, int count, DataInputStream is) throws IOException {
		int code;
		byte[] vendor = new byte[4];
		byte[] name = new byte[8];
		for (int i = 0; i < count; i++) {
			code = is.readInt();
			is.read(vendor);
			is.read(name);
			caps.enable(new CapabilityInfo(code, vendor, name));
		}
	}

}
