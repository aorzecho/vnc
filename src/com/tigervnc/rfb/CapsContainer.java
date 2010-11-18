//
//  Copyright (C) 2003 Constantin Kaplinsky.  All Rights Reserved.
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
// CapsContainer.java - A container of capabilities as used in the RFB
// protocol 3.130
//

package com.tigervnc.rfb;

import java.util.Vector;
import java.util.Hashtable;


public class CapsContainer {

  // Public methods

  public CapsContainer() {
    infoMap = new Hashtable(64, (float)0.25);
    orderedList = new Vector(32, 8);
  }

  public void add(CapabilityInfo capinfo) {
    Integer key = new Integer(capinfo.getCode());
    infoMap.put(key, capinfo);
  }

  public void add(int code, String vendor, String name, String desc) {
    Integer key = new Integer(code);
    infoMap.put(key, new CapabilityInfo(code, vendor, name, desc));
  }

  public boolean isKnown(int code) {
    return infoMap.containsKey(new Integer(code));
  }

  public CapabilityInfo getInfo(int code) {
    return (CapabilityInfo)infoMap.get(new Integer(code));
  }

  public String getDescription(int code) {
    CapabilityInfo capinfo = (CapabilityInfo)infoMap.get(new Integer(code));
    if (capinfo == null)
      return null;

    return capinfo.getDescription();
  }

  public boolean enable(CapabilityInfo other) {
    Integer key = new Integer(other.getCode());
    CapabilityInfo capinfo = (CapabilityInfo)infoMap.get(key);
    if (capinfo == null)
      return false;

    boolean enabled = capinfo.enableIfEquals(other);
    if (enabled)
      orderedList.addElement(key);

    return enabled;
  }

  public boolean isEnabled(int code) {
    CapabilityInfo capinfo = (CapabilityInfo)infoMap.get(new Integer(code));
    if (capinfo == null)
      return false;

    return capinfo.isEnabled();
  }

  public int numEnabled() {
    return orderedList.size();
  }

  public int getByOrder(int idx) {
    int code;
    try {
      code = ((Integer)orderedList.elementAt(idx)).intValue();
    } catch (ArrayIndexOutOfBoundsException e) {
      code = 0;
    }
    return code;
  }

  // Protected data

  protected Hashtable infoMap;
  protected Vector orderedList;
  
  private static CapsContainer authCaps;
  private static CapsContainer serverCaps;
  private static CapsContainer clientCaps;
  private static CapsContainer encodingCaps;
  
  static{
	  authCaps = new CapsContainer();
	  authCaps.add(Encodings.AuthNone, Encodings.StandardVendor, Encodings.SigAuthNone, "No authentication");
	  authCaps.add(Encodings.AuthVNC, Encodings.StandardVendor, Encodings.SigAuthVNC, "Standard VNC password authentication");
	  
	  serverCaps = new CapsContainer();
	  serverCaps.add(Encodings.EndOfContinuousUpdates, Encodings.TightVncVendor,	Encodings.SigEndOfContinuousUpdates, "End of continuous updates notification");
	
	  clientCaps = new CapsContainer();
	  clientCaps.add(Encodings.EnableContinuousUpdates, Encodings.TightVncVendor, Encodings.SigEnableContinuousUpdates, "Enable/disable continuous updates");
	  clientCaps.add(Encodings.VideoRectangleSelection, Encodings.TightVncVendor, Encodings.SigVideoRectangleSelection, "Select a rectangle to be treated as video");
	  clientCaps.add(Encodings.VideoFreeze, Encodings.TightVncVendor, Encodings.SigVideoFreeze, "Disable/enable video rectangle");
	  
	  encodingCaps = new CapsContainer();
	  encodingCaps.add(Encodings.EncodingCopyRect, Encodings.StandardVendor, Encodings.SigEncodingCopyRect, "Standard CopyRect encoding");
	  encodingCaps.add(Encodings.EncodingRRE, Encodings.StandardVendor, Encodings.SigEncodingRRE, "Standard RRE encoding");
	  encodingCaps.add(Encodings.EncodingCoRRE, Encodings.StandardVendor, Encodings.SigEncodingCoRRE, "Standard CoRRE encoding");
	  encodingCaps.add(Encodings.EncodingHextile, Encodings.StandardVendor, Encodings.SigEncodingHextile, "Standard Hextile encoding");
	  encodingCaps.add(Encodings.EncodingZRLE, Encodings.StandardVendor, Encodings.SigEncodingZRLE, "Standard ZRLE encoding");
	  encodingCaps.add(Encodings.EncodingZlib, Encodings.TridiaVncVendor, Encodings.SigEncodingZlib, "Zlib encoding");
	  encodingCaps.add(Encodings.EncodingTight, Encodings.TightVncVendor, Encodings.SigEncodingTight, "Tight encoding");
		// Supported pseudo-encoding types
	  encodingCaps.add(Encodings.EncodingCompressLevel0, Encodings.TightVncVendor,Encodings.SigEncodingCompressLevel0, "Compression level");
	  encodingCaps.add(Encodings.EncodingQualityLevel0, Encodings.TightVncVendor,	Encodings.SigEncodingQualityLevel0, "JPEG quality level");
	  encodingCaps.add(Encodings.EncodingXCursor, Encodings.TightVncVendor, Encodings.SigEncodingXCursor, "X-style cursor shape update");
	  encodingCaps.add(Encodings.EncodingRichCursor, Encodings.TightVncVendor, Encodings.SigEncodingRichCursor, "Rich-color cursor shape update");
	  encodingCaps.add(Encodings.EncodingPointerPos, Encodings.TightVncVendor, Encodings.SigEncodingPointerPos, "Pointer position update");
	  encodingCaps.add(Encodings.EncodingLastRect, Encodings.TightVncVendor, Encodings.SigEncodingLastRect, "LastRect protocol extension");
	  encodingCaps.add(Encodings.EncodingNewFBSize, Encodings.TightVncVendor, Encodings.SigEncodingNewFBSize, "Framebuffer size change");

	  
  }
  public static CapsContainer getAuthCapabilities(){
	  return authCaps;
  }
  public static CapsContainer getServerCapabilities(){
	  return serverCaps;
  }
  public static CapsContainer getClientCapabilities(){
	  return clientCaps;
  }
  public static CapsContainer getEncodingCapabilities(){
	  return encodingCaps;
  }
}

