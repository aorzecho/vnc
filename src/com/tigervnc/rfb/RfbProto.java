//
//  Copyright (C) 2001-2004 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2001-2006 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//

//
// RfbProto.java
//

package com.tigervnc.rfb;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import com.tigervnc.rfb.message.Authentication;
import com.tigervnc.rfb.message.ClientCutText;
import com.tigervnc.rfb.message.FramebufferUpdateRequest;
import com.tigervnc.rfb.message.KeyboardEvent;
import com.tigervnc.rfb.message.ServerInit;
import com.tigervnc.rfb.message.SetColorMapEntries;
import com.tigervnc.rfb.message.SetEncodings;
import com.tigervnc.rfb.message.SetPixelFormat;
import com.tigervnc.rfb.message.Version;
import com.tigervnc.rfb.message.KeyboardEvent.KeyUndefinedException;
import com.tigervnc.vncviewer.SocketFactory;
import com.tigervnc.vncviewer.VncViewer;

public class RfbProto {

	String host;
	int port;
	Socket sock;
	static OutputStream os;
	public SessionRecorder rec;
	public boolean inNormalProtocol = false;
	public VncViewer viewer;

	// Input stream is declared private to make sure it can be accessed
	// only via RfbProto methods. We have to do this because we want to
	// count how many bytes were read.
	private DataInputStream is;
	private long numBytesRead = 0;

	public long getNumBytesRead() {
		return numBytesRead;
	}

	// Java on UNIX does not call keyPressed() on some keys, for example
	// swedish keys To prevent our workaround to produce duplicate
	// keypresses on JVMs that actually works, keep track of if
	// keyPressed() for a "broken" key was called or not.
	//boolean brokenKeyPressed = false;

	// This will be set to true on the first framebuffer update
	// containing Zlib-, ZRLE- or Tight-encoded data.
	boolean wereZlibUpdates = false;

	// This fields are needed to show warnings about inefficiently saved
	// sessions only once per each saved session file.
	boolean zlibWarningShown;
	boolean tightWarningShown;

	// Before starting to record each saved session, we set this field
	// to 0, and increment on each framebuffer update. We don't flush
	// the SessionRecorder data into the file before the second update.
	// This allows us to write initial framebuffer update with zero
	// timestamp, to let the player show initial desktop before
	// playback.
	int numUpdatesInSession;

	// Measuring network throughput.
	boolean timing;
	long timeWaitedIn100us;
	long timedKbits;

	// Protocol version and TightVNC-specific protocol options.
	public Version clientVersion;
	public Version serverVersion;

	boolean protocolTightVNC;
	CapsContainer tunnelCaps, authCaps;
	CapsContainer serverMsgCaps;

	public CapsContainer clientMsgCaps;
	CapsContainer encodingCaps;

	// "Continuous updates" is a TightVNC-specific feature that allows
	// receiving framebuffer updates continuously, without sending update
	// requests. The variables below track the state of this feature.
	// Initially, continuous updates are disabled. They can be enabled
	// by calling tryEnableContinuousUpdates() method, and only if this
	// feature is supported by the server. To disable continuous updates,
	// tryDisableContinuousUpdates() should be called.
	private boolean continuousUpdatesActive = false;
	private boolean continuousUpdatesEnding = false;

	// If true, informs that the RFB socket was closed.
	private boolean closed;

	public ServerInit server;
	//
	// Constructor. Make TCP connection to RFB server.
	//

	public RfbProto(String h, int p, VncViewer v) throws IOException {
		viewer = v;
		host = h;
		port = p;

		if (viewer.socketFactory == null) {
			sock = new Socket(host, port);
			sock.setTcpNoDelay(true);
		} else {
			try {
				Class factoryClass = Class.forName(viewer.socketFactory);
				SocketFactory factory = (SocketFactory) factoryClass
						.newInstance();
				if (viewer.inAnApplet)
					sock = factory.createSocket(host, port, viewer);
				else
					sock = factory.createSocket(host, port, viewer.mainArgs);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}
		}
		is = new DataInputStream(new BufferedInputStream(sock.getInputStream(),
				16384));
		os = sock.getOutputStream();

		timing = false;
		timeWaitedIn100us = 5;
		timedKbits = 0;
	}

	public synchronized void close() {
		try {
			sock.close();
			closed = true;
			System.out.println("RFB socket closed");
			if (rec != null) {
				rec.close();
				rec = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized boolean closed() {
		return closed;
	}

	/////////////////////////
	// Messages from server
	//////////////////////// 
	
	public void readVersionMsg() throws Exception {
		serverVersion = new Version(is);
	}
	
	public void readServerInit() throws IOException {
		server = new ServerInit(is, protocolTightVNC);
		clientMsgCaps = server.clientCaps;
		serverMsgCaps = server.serverCaps;
		encodingCaps = server.encodingTypes;
		

		if (!clientMsgCaps.isEnabled(Encodings.EnableContinuousUpdates)) {
			viewer.options.disableContUpdates();
		}

		inNormalProtocol = true;
	}
	
	//
	// Read security type from the server (protocol version 3.3).
	//

	int readSecurityType() throws Exception {
		int secType = readU32();

		switch (secType) {
		case Encodings.SecTypeInvalid:
			readConnFailedReason();
			return Encodings.SecTypeInvalid; // should never be executed
		case Encodings.SecTypeNone:
		case Encodings.SecTypeVncAuth:
			return secType;
		default:
			throw new Exception("Unknown security type from RFB server: "
					+ secType);
		}
	}
	
	//
	// Read security result.
	// Throws an exception on authentication failure.
	//
	void readSecurityResult(String authType) throws Exception {
		int securityResult = readU32();

		switch (securityResult) {
		case Encodings.VncAuthOK:
			System.out.println(authType + ": success");
			break;
		case Encodings.VncAuthFailed:
			if (clientVersion.getMinor() >= 8)
				readConnFailedReason();
			throw new Exception(authType + ": failed");
		case Encodings.VncAuthTooMany:
			throw new Exception(authType + ": failed, too many tries");
		default:
			throw new Exception(authType + ": unknown result " + securityResult);
		}
	}
	
	//
	// Read the string describing the reason for a connection failure,
	// and throw an exception.
	//

	void readConnFailedReason() throws Exception {
		int reasonLen = readU32();
		byte[] reason = new byte[reasonLen];
		readFully(reason);
		throw new Exception(new String(reason));
	}
	
	//
	// Read the server message type
	//

	public int readServerMessageType() throws IOException {
		int msgType = readU8();

		// If the session is being recorded:
		if (rec != null) {
			if (msgType == Encodings.Bell) { // Save Bell messages in session files.
				rec.writeByte(msgType);
				if (numUpdatesInSession > 0)
					rec.flush();
			}
		}

		return msgType;
	}
	
	public void readFramebufferUpdate() throws IOException {
		// *FramebufferUpdate*
		skipBytes(1); // *padding*
		updateNRects = readU16(); // *number-of-rectangles*

		// If the session is being recorded:
		if (rec != null) {
			rec.writeByte(Encodings.FramebufferUpdate);
			rec.writeByte(0);
			rec.writeShortBE(updateNRects);
		}

		numUpdatesInSession++;
	}
	
	
	//////////////////////////////////////
	// Messages to the server
	/////////////////////////////////////


	//
	// Write our protocol version message
	//

	public void writeVersionMsg() throws IOException {
		if(serverVersion.getMajor() > 3 || serverVersion.getMinor() >= 8){
			clientVersion = new Version(3, 8);
		}
		else if(serverVersion.getMinor() >= 7){
			clientVersion = new Version(3, 7);
		}
		else{
			clientVersion = new Version(3,3);
		}
		os.write(clientVersion.getBytes());
		protocolTightVNC = false;
		initCapabilities();
	}
	
	public void writeKeyboardEvent(KeyEvent evt) throws IOException, KeyUndefinedException{
		os.write(new KeyboardEvent(evt).getBytes());
	}
	
	public void writeKeyboardEvent(int keysym, int keycode, boolean press) throws IOException{
		os.write(new KeyboardEvent(keysym, keycode, press).getBytes());
	}
	
	public void writePointerEvent(MouseEvent evt) throws IOException {
		// TODO: move into pointer event
		int modifiers = evt.getModifiers();

		int mask2 = 2;
		int mask3 = 4;
		if (viewer.options.reverseMouseButtons2And3) {
			mask2 = 4;
			mask3 = 2;
		}

		// Note: For some reason, AWT does not set BUTTON1_MASK on left
		// button presses. Here we think that it was the left button if
		// modifiers do not include BUTTON2_MASK or BUTTON3_MASK.

		if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
			if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
				pointerMask = mask2;
				modifiers &= ~ALT_MASK;
			} else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
				pointerMask = mask3;
				modifiers &= ~META_MASK;
			} else {
				pointerMask = 1;
			}
		} else if (evt.getID() == MouseEvent.MOUSE_RELEASED) {
			pointerMask = 0;
			if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
				modifiers &= ~ALT_MASK;
			} else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
				modifiers &= ~META_MASK;
			}
		}

		eventBufLen = 0;
		//writeModifierKeyEvents(modifiers);

		int x = evt.getX();
		int y = evt.getY();

		if (x < 0)
			x = 0;
		if (y < 0)
			y = 0;

		eventBuf[eventBufLen++] = (byte) Encodings.POINTER_EVENT;
		eventBuf[eventBufLen++] = (byte) pointerMask;
		eventBuf[eventBufLen++] = (byte) ((x >> 8) & 0xff);
		eventBuf[eventBufLen++] = (byte) (x & 0xff);
		eventBuf[eventBufLen++] = (byte) ((y >> 8) & 0xff);
		eventBuf[eventBufLen++] = (byte) (y & 0xff);

		//
		// Always release all modifiers after an "up" event
		//

		if (pointerMask == 0) {
			//writeModifierKeyEvents(0);
		}

		os.write(eventBuf, 0, eventBufLen);
	}

	//
	// Negotiate the authentication scheme.
	//

	public int negotiateSecurity() throws Exception {
		return (clientVersion.getMinor() >= 7) ? selectSecurityType() : readSecurityType();
	}

	

	//
	// Select security type from the server's list (protocol versions 3.7/3.8).
	//

	int selectSecurityType() throws Exception {
		int secType = Encodings.SecTypeInvalid;

		// Read the list of secutiry types.
		int nSecTypes = readU8();
		if (nSecTypes == 0) {
			readConnFailedReason();
			return Encodings.SecTypeInvalid; // should never be executed
		}
		byte[] secTypes = new byte[nSecTypes];
		readFully(secTypes);

		// Find out if the server supports TightVNC protocol extensions
		for (int i = 0; i < nSecTypes; i++) {
			if (secTypes[i] == Encodings.SecTypeTight) {
				System.out.println("TightVNC protocol");
				protocolTightVNC = true;
				os.write(Encodings.SecTypeTight);
				return Encodings.SecTypeTight;
			}
		}

		// Find first supported security type.
		for (int i = 0; i < nSecTypes; i++) {
			if (secTypes[i] == Encodings.SecTypeNone || secTypes[i] == Encodings.SecTypeVncAuth) {
				secType = secTypes[i];
				break;
			}
		}

		if (secType == Encodings.SecTypeInvalid) {
			throw new Exception("Server did not offer supported security type");
		} else {
			os.write(secType);
		}

		return secType;
	}

	//
	// Perform "no authentication".
	//

	public void authenticateNone() throws Exception {
		if (clientVersion.getMinor() >= 8)
			readSecurityResult("No authentication");
	}

	//
	// Perform standard VNC Authentication.
	//

	public void authenticateVNC(String pw) throws Exception {
		os.write(new Authentication(is, pw).getBytes());
		readSecurityResult("VNC authentication");
	}

	

	

	//
	// Initialize capability lists (TightVNC protocol extensions).
	//

	void initCapabilities() {
		tunnelCaps = new CapsContainer();
		authCaps = CapsContainer.getAuthCapabilities();	
		serverMsgCaps = CapsContainer.getServerCapabilities();
		clientMsgCaps = CapsContainer.getClientCapabilities();
		encodingCaps = CapsContainer.getEncodingCapabilities();
	}

	//
	// Setup tunneling (TightVNC protocol extensions)
	//

	public void setupTunneling() throws IOException {
		int nTunnelTypes = readU32();
		if (nTunnelTypes != 0) {
			readCapabilityList(tunnelCaps, nTunnelTypes);

			// We don't support tunneling yet.
			writeInt(Encodings.NoTunneling);
		}
	}

	//
	// Negotiate authentication scheme (TightVNC protocol extensions)
	//

	public int negotiateAuthenticationTight() throws Exception {
		int nAuthTypes = readU32();
		if (nAuthTypes == 0)
			return Encodings.AuthNone;

		readCapabilityList(authCaps, nAuthTypes);
		for (int i = 0; i < authCaps.numEnabled(); i++) {
			int authType = authCaps.getByOrder(i);
			if (authType == Encodings.AuthNone || authType == Encodings.AuthVNC) {
				writeInt(authType);
				return authType;
			}
		}
		throw new Exception("No suitable authentication scheme found");
	}

	//
	// Read a capability list (TightVNC protocol extensions)
	//

	void readCapabilityList(CapsContainer caps, int count) throws IOException {
		int code;
		byte[] vendor = new byte[4];
		byte[] name = new byte[8];
		for (int i = 0; i < count; i++) {
			code = readU32();
			readFully(vendor);
			readFully(name);
			caps.enable(new CapabilityInfo(code, vendor, name));
		}
	}

	//
	// Write a 32-bit integer into the output stream.
	//

	void writeInt(int value) throws IOException {
		byte[] b = new byte[4];
		b[0] = (byte) ((value >> 24) & 0xff);
		b[1] = (byte) ((value >> 16) & 0xff);
		b[2] = (byte) ((value >> 8) & 0xff);
		b[3] = (byte) (value & 0xff);
		os.write(b);
	}

	//
	// Write the client initialisation message
	//

	public void writeClientInit() throws IOException {
		if (viewer.options.shareDesktop) {
			os.write(1);
		} else {
			os.write(0);
		}
		viewer.options.disableShareDesktop();
	}

	private String desktopName = "";

	//
	// Create session file and write initial protocol messages into it.
	//
	public void startSession(String fname) throws IOException {
		rec = new SessionRecorder(fname);
		rec.writeHeader();
		rec.write(Encodings.versionMsg_3_3.getBytes());
		rec.writeIntBE(Encodings.SecTypeNone);
		rec.writeShortBE(server.fb_width);
		rec.writeShortBE(server.fb_height);
		byte[] fbsServerInitMsg = { 32, 24, 0, 1, 0, (byte) 0xFF, 0,
				(byte) 0xFF, 0, (byte) 0xFF, 16, 8, 0, 0, 0, 0 };
		rec.write(fbsServerInitMsg);
		rec.writeIntBE(desktopName.length());
		rec.write(desktopName.getBytes());
		numUpdatesInSession = 0;

		// FIXME: If there were e.g. ZRLE updates only, that should not
		// affect recording of Zlib and Tight updates. So, actually
		// we should maintain separate flags for Zlib, ZRLE and
		// Tight, instead of one ``wereZlibUpdates'' variable.
		//

		zlibWarningShown = false;
		tightWarningShown = false;
	}

	//
	// Close session file.
	//

	public void closeSession() throws IOException {
		if (rec != null) {
			rec.close();
			rec = null;
		}
	}

	//
	// Set new framebuffer size
	//

	public void setFramebufferSize(int width, int height) {
		server.fb_width = width;
		server.fb_height = height;
	}

	

	//
	// Read a FramebufferUpdate message
	//

	public int updateNRects;

	

	//
	// Returns true if encoding is not pseudo
	//
	// FIXME: Find better way to differ pseudo and real encodings
	//

	boolean isRealDecoderEncoding(int encoding) {
		if ((encoding >= 1) && (encoding <= 16)) {
			return true;
		}
		return false;
	}

	// Read a FramebufferUpdate rectangle header

	public int updateRectX;

	public int updateRectY;

	public int updateRectW;

	public int updateRectH;

	public int updateRectEncoding;

	public void readFramebufferUpdateRectHdr() throws Exception {
		updateRectX = readU16();
		updateRectY = readU16();
		updateRectW = readU16();
		updateRectH = readU16();
		updateRectEncoding = readU32();

		if (updateRectEncoding == Encodings.EncodingZlib
				|| updateRectEncoding == Encodings.EncodingZRLE
				|| updateRectEncoding == Encodings.EncodingTight)
			wereZlibUpdates = true;

		// If the session is being recorded:
		if (rec != null) {
			if (numUpdatesInSession > 1)
				rec.flush(); // Flush the output on each rectangle.
			rec.writeShortBE(updateRectX);
			rec.writeShortBE(updateRectY);
			rec.writeShortBE(updateRectW);
			rec.writeShortBE(updateRectH);

			//
			// If this is pseudo encoding or CopyRect that write encoding ID
			// in this place. All real encoding ID will be written to record
			// stream
			// in decoder classes.

			if (((!isRealDecoderEncoding(updateRectEncoding))) && (rec != null)) {
				rec.writeIntBE(updateRectEncoding);
			}
		}

		if (updateRectEncoding < 0 || updateRectEncoding > Encodings.MaxNormalEncoding)
			return;

		if (updateRectX + updateRectW > server.fb_width
				|| updateRectY + updateRectH > server.fb_height) {
			throw new Exception("Framebuffer update rectangle too large: "
					+ updateRectW + "x" + updateRectH + " at (" + updateRectX
					+ "," + updateRectY + ")");
		}
	}

	//
	// Read a ServerCutText message
	//

	public String readServerCutText() throws IOException {
		skipBytes(3);
		int len = readU32();
		byte[] text = new byte[len];
		readFully(text);
		return new String(text);
	}

	//
	// Read an integer in compact representation (1..3 bytes).
	// Such format is used as a part of the Tight encoding.
	// Also, this method records data if session recording is active and
	// the viewer's recordFromBeginning variable is set to true.
	//

	public int readCompactLen() throws IOException {
		int[] portion = new int[3];
		portion[0] = readU8();
		int byteCount = 1;
		int len = portion[0] & 0x7F;
		if ((portion[0] & 0x80) != 0) {
			portion[1] = readU8();
			byteCount++;
			len |= (portion[1] & 0x7F) << 7;
			if ((portion[1] & 0x80) != 0) {
				portion[2] = readU8();
				byteCount++;
				len |= (portion[2] & 0xFF) << 14;
			}
		}

		return len;
	}
	
	public int authenticateVeNCrypt() throws Exception {
		int majorVersion = readU8();
		int minorVersion = readU8();
		int Version = (majorVersion << 8) | minorVersion;
		if (Version < 0x0002) {
		    os.write(0);
		    os.write(0);
		    throw new Exception("Server reported an unsupported VeNCrypt version");
		}
		os.write(0);
		os.write(2);
		if (readU8() != 0)
		    throw new Exception("Server reported it could not support the VeNCrypt version");
		int nSecTypes = readU8();
		int[] secTypes = new int[nSecTypes];
		for(int i = 0; i < nSecTypes; i++)
		    secTypes[i] = readU32();

		for(int i = 0; i < nSecTypes; i++)
		    switch(secTypes[i])
			{
			case Encodings.SecTypeNone:
			case Encodings.SecTypeVncAuth:
			case Encodings.SecTypePlain:
			    writeInt(secTypes[i]);
			    return secTypes[i];
			}

		throw new Exception("No valid VeNCrypt sub-type");
	    }

	public void resendFramebufferUpdateRequest() throws IOException {
		writeFramebufferUpdateRequest(0, 0, server.fb_width,
				server.fb_height, false);
	}

	//
	// Write a FramebufferUpdateRequest message
	//

	public void writeFramebufferUpdateRequest(int x, int y, int w, int h,
			boolean incremental) throws IOException {
		FramebufferUpdateRequest req = new FramebufferUpdateRequest(incremental, x, y, w, h);
		os.write(req.getBytes());
	}
	
	public void authenticatePlain(String User, String Password) throws Exception {
	      byte[] user=User.getBytes();
	      byte[] password=Password.getBytes();
	      writeInt(user.length);
	      writeInt(password.length);
	      os.write(user);
	      os.write(password);

	      readSecurityResult("Plain authentication");
	    }

	//
	// Write a SetPixelFormat message
	//

	public void writeSetPixelFormat(int bits_per_pixel, int depth, boolean big_endian,
			boolean true_color, int red_max, int green_max, int blue_max,
			int red_shift, int green_shift, int blue_shift) throws IOException {
		os.write(new SetPixelFormat(bits_per_pixel, depth, big_endian, true_color, red_max, green_max, blue_max, red_shift, green_shift, blue_shift).getBytes());
	}
	
	//
	// Write a SetColourMapEntries message. The values in the red, green and
	// blue arrays are from 0 to 65535.
	//

	void writeSetColourMapEntries(int firstColour, int nColours, int[] red,
			int[] green, int[] blue) throws IOException {
		os.write(new SetColorMapEntries(firstColour, nColours, red, green, blue).getBytes());
	}

	//
	// Write a SetEncodings message
	//

	public void writeSetEncodings(List<Integer> encs) throws IOException {
		os.write(new SetEncodings(encs).getBytes());
	}

	//
	// Write a ClientCutText message
	//

	public void writeClientCutText(String text) throws IOException {
		os.write(new ClientCutText(text).getBytes());
	}

	//
	// A buffer for putting pointer and keyboard events before being sent. This
	// is to ensure that multiple RFB events generated from a single Java Event
	// will all be sent in a single network packet. The maximum possible
	// length is 4 modifier down events, a single key event followed by 4
	// modifier up events i.e. 9 key events or 72 bytes.
	//

	byte[] eventBuf = new byte[72];
	int eventBufLen;

	// Useful shortcuts for modifier masks.

	final static int CTRL_MASK = InputEvent.CTRL_MASK;
	final static int SHIFT_MASK = InputEvent.SHIFT_MASK;
	final static int META_MASK = InputEvent.META_MASK;
	final static int ALT_MASK = InputEvent.ALT_MASK;

	//
	// Write a pointer event message. We may need to send modifier key events
	// around it to set the correct modifier state.
	//

	int pointerMask = 0;

	//
	// Enable continuous updates for the specified area of the screen (but
	// only if EnableContinuousUpdates message is supported by the server).
	//

	public void tryEnableContinuousUpdates(int x, int y, int w, int h)
			throws IOException {
		if (!clientMsgCaps.isEnabled(Encodings.EnableContinuousUpdates)) {
			System.out
					.println("Continuous updates not supported by the server");
			return;
		}

		if (continuousUpdatesActive) {
			System.out.println("Continuous updates already active");
			return;
		}

		byte[] b = new byte[10];

		b[0] = (byte) Encodings.EnableContinuousUpdates;
		b[1] = (byte) 1; // enable
		b[2] = (byte) ((x >> 8) & 0xff);
		b[3] = (byte) (x & 0xff);
		b[4] = (byte) ((y >> 8) & 0xff);
		b[5] = (byte) (y & 0xff);
		b[6] = (byte) ((w >> 8) & 0xff);
		b[7] = (byte) (w & 0xff);
		b[8] = (byte) ((h >> 8) & 0xff);
		b[9] = (byte) (h & 0xff);

		os.write(b);

		continuousUpdatesActive = true;
		System.out.println("Continuous updates activated");
	}
	
	public void releaseAllKeys() throws IOException{
		for (Map.Entry<Integer, Integer> e : KeyboardEvent.getPressedKeys().entrySet()) {
			os.write(new KeyboardEvent(e.getValue(), e.getKey(), false).getBytes());
		}
		KeyboardEvent.clearPressedKeys();
	}
	
	

	//
	// Disable continuous updates (only if EnableContinuousUpdates message
	// is supported by the server).
	//

	public void tryDisableContinuousUpdates() throws IOException {
		if (!clientMsgCaps.isEnabled(Encodings.EnableContinuousUpdates)) {
			System.out
					.println("Continuous updates not supported by the server");
			return;
		}

		if (!continuousUpdatesActive) {
			System.out.println("Continuous updates already disabled");
			return;
		}

		if (continuousUpdatesEnding)
			return;

		byte[] b = { (byte) Encodings.EnableContinuousUpdates, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		os.write(b);

		if (!serverMsgCaps.isEnabled(Encodings.EndOfContinuousUpdates)) {
			// If the server did not advertise support for the
			// EndOfContinuousUpdates message (should not normally happen
			// when EnableContinuousUpdates is supported), then we clear
			// 'continuousUpdatesActive' variable immediately. Normally,
			// it will be reset on receiving EndOfContinuousUpdates message
			// from the server.
			continuousUpdatesActive = false;
		} else {
			// Indicate that we are waiting for EndOfContinuousUpdates.
			continuousUpdatesEnding = true;
		}
	}

	//
	// Process EndOfContinuousUpdates message received from the server.
	//

	public void endOfContinuousUpdates() {
		continuousUpdatesActive = false;
		continuousUpdatesEnding = false;
	}

	//
	// Check if continuous updates are in effect.
	//

	public boolean continuousUpdatesAreActive() {
		return continuousUpdatesActive;
	}

	/**
	 * Send a rectangle selection to be treated as video by the server (but only
	 * if VideoRectangleSelection message is supported by the server).
	 * 
	 * @param rect
	 *            specifies coordinates and size of the rectangule.
	 * @throws java.io.IOException
	 */
	public void trySendVideoSelection(Rectangle rect) throws IOException {
		if (!clientMsgCaps.isEnabled(Encodings.VideoRectangleSelection)) {
			System.out
					.println("Video area selection is not supported by the server");
			return;
		}

		// Send zero coordinates if the rectangle is empty.
		if (rect.isEmpty()) {
			rect = new Rectangle();
		}

		int x = rect.x;
		int y = rect.y;
		int w = rect.width;
		int h = rect.height;

		byte[] b = new byte[10];

		b[0] = (byte) Encodings.VideoRectangleSelection;
		b[1] = (byte) 0; // reserved
		b[2] = (byte) ((x >> 8) & 0xff);
		b[3] = (byte) (x & 0xff);
		b[4] = (byte) ((y >> 8) & 0xff);
		b[5] = (byte) (y & 0xff);
		b[6] = (byte) ((w >> 8) & 0xff);
		b[7] = (byte) (w & 0xff);
		b[8] = (byte) ((h >> 8) & 0xff);
		b[9] = (byte) (h & 0xff);

		os.write(b);

		System.out.println("Video rectangle selection message sent");
	}

	public void trySendVideoFreeze(boolean freeze) throws IOException {
		if (!clientMsgCaps.isEnabled(Encodings.VideoFreeze)) {
			System.out.println("Video freeze is not supported by the server");
			return;
		}

		byte[] b = new byte[2];
		byte fb = 0;
		if (freeze) {
			fb = 1;
		}

		b[0] = (byte) Encodings.VideoFreeze;
		b[1] = (byte) fb;

		os.write(b);

		System.out.println("Video freeze selection message sent");
	}

	public void startTiming() {
		timing = true;

		// Carry over up to 1s worth of previous rate for smoothing.

		if (timeWaitedIn100us > 10000) {
			timedKbits = timedKbits * 10000 / timeWaitedIn100us;
			timeWaitedIn100us = 10000;
		}
	}

	public void stopTiming() {
		timing = false;
		if (timeWaitedIn100us < timedKbits / 2)
			timeWaitedIn100us = timedKbits / 2; // upper limit 20Mbit/s
	}

	public long kbitsPerSecond() {
		return timedKbits * 10000 / timeWaitedIn100us;
	}

	public long timeWaited() {
		return timeWaitedIn100us;
	}

	//
	// Methods for reading data via our DataInputStream member variable (is).
	//
	// In addition to reading data, the readFully() methods updates variables
	// used to estimate data throughput.
	//

	public void readFully(byte b[]) throws IOException {
		readFully(b, 0, b.length);
	}

	public void readFully(byte b[], int off, int len) throws IOException {
		long before = 0;
		if (timing)
			before = System.currentTimeMillis();

		is.readFully(b, off, len);

		if (timing) {
			long after = System.currentTimeMillis();
			long newTimeWaited = (after - before) * 10;
			int newKbits = len * 8 / 1000;

			// limit rate to between 10kbit/s and 40Mbit/s

			if (newTimeWaited > newKbits * 1000)
				newTimeWaited = newKbits * 1000;
			if (newTimeWaited < newKbits / 4)
				newTimeWaited = newKbits / 4;

			timeWaitedIn100us += newTimeWaited;
			timedKbits += newKbits;
		}

		numBytesRead += len;
	}

	public final int available() throws IOException {
		return is.available();
	}

	// FIXME: DataInputStream::skipBytes() is not guaranteed to skip
	// exactly n bytes. Probably we don't want to use this method.
	public final int skipBytes(int n) throws IOException {
		int r = is.skipBytes(n);
		numBytesRead += r;
		return r;
	}

	public final int readU8() throws IOException {
		int r = is.readUnsignedByte();
		numBytesRead++;
		return r;
	}

	public final int readU16() throws IOException {
		int r = is.readUnsignedShort();
		numBytesRead += 2;
		return r;
	}

	public final int readU32() throws IOException {
		int r = is.readInt();
		numBytesRead += 4;
		return r;
	}
}
