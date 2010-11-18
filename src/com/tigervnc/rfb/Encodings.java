package com.tigervnc.rfb;

/**
 * Encodings
 */
public class Encodings {

	public final static int QEMU = 255;

	public final static int SET_PIXEL_FORMAT = 0;
	public final static int SET_COLOR_MAP_ENTRIES = 1;
	public final static int SET_ENCODINGS = 2;
	public final static int FRAME_BUFFER_UPDATE_REQUEST = 3;
	public final static int KEYBOARD_EVENT = 4;
	public final static int POINTER_EVENT = 5;
	public final static int CLIENT_CUT_TEXT = 6;

	public final static String versionMsg_3_3 = "RFB 003.003\n";
	public final static String	versionMsg_3_7 = "RFB 003.007\n";
	public final static String versionMsg_3_8 = "RFB 003.008\n";

	// Vendor signatures: standard VNC/RealVNC, TridiaVNC, and TightVNC
	public final static String StandardVendor = "STDV"; 
	public final static String TridiaVncVendor = "TRDV";
	public final static String TightVncVendor = "TGHT";

	// Security types
	  
	public final static int
	    SecTypeInvalid   = 0,
	    SecTypeNone      = 1,
	    SecTypeVncAuth   = 2,
	    SecTypeTight     = 16,
	    SecTypeVeNCrypt  = 19,
	    SecTypePlain     = 256,
	    SecTypeTLSNone   = 257,
	    SecTypeTLSVnc    = 258,
	    SecTypeTLSPlain  = 259,
	    SecTypeX509None  = 260,
	    SecTypeX509Vnc   = 261,
	    SecTypeX509Plain = 262;

	// Supported tunneling types
	public final static int NoTunneling = 0;
	public final static String SigNoTunneling = "NOTUNNEL";

	// Supported authentication types
	public final static int AuthNone = 1, AuthVNC = 2, AuthUnixLogin = 129;
	public final static String SigAuthNone = "NOAUTH__",
			SigAuthVNC = "VNCAUTH_", SigAuthUnixLogin = "ULGNAUTH";

	// VNC authentication results
	public final static int VncAuthOK = 0, VncAuthFailed = 1,
			VncAuthTooMany = 2;

	// Standard server-to-client messages
	public final static int FramebufferUpdate = 0;

	public final static int SetColourMapEntries = 1, Bell = 2,
			ServerCutText = 3;

	// Non-standard server-to-client messages
	public final static int EndOfContinuousUpdates = 150;
	public final static String SigEndOfContinuousUpdates = "CUS_EOCU";

	// Non-standard client-to-server messages
	public final static int EnableContinuousUpdates = 150;
	public final static int VideoRectangleSelection = 151;
	public final static int VideoFreeze = 152;
	public final static String SigVideoFreeze = "VD_FREEZ";
	public final static String SigEnableContinuousUpdates = "CUC_ENCU";
	public final static String SigVideoRectangleSelection = "VRECTSEL";

	public final static int EXTENDED_KEY_EVENT = 0;

	// Supported encodings and pseudo-encodings
	public final static int EncodingRaw = 0;

	public final static int EncodingCopyRect = 1;

	public static final int EncodingRRE = 2;

	public final static int EncodingCoRRE = 4;

	public final static int EncodingHextile = 5;

	public final static int EncodingZlib = 6;

	public final static int EncodingTight = 7;

	public static final int EncodingZRLE = 16;

	public final static int EncodingCompressLevel0 = 0xFFFFFF00,
			EncodingQualityLevel0 = 0xFFFFFFE0;

	public static final int EncodingXCursor = 0xFFFFFF10;

	public final static int EncodingRichCursor = 0xFFFFFF11;

	public final static int EncodingPointerPos = 0xFFFFFF18;

	public static final int EncodingLastRect = 0xFFFFFF20;

	public final static int EncodingNewFBSize = 0xFFFFFF21;

	public final static int ENCODING_EXTENDED_KEY_EVENT = -258;
	public final static String SigEncodingRaw = "RAW_____",
			SigEncodingCopyRect = "COPYRECT", SigEncodingRRE = "RRE_____",
			SigEncodingCoRRE = "CORRE___", SigEncodingHextile = "HEXTILE_",
			SigEncodingZlib = "ZLIB____", SigEncodingTight = "TIGHT___",
			SigEncodingZRLE = "ZRLE____",
			SigEncodingCompressLevel0 = "COMPRLVL",
			SigEncodingQualityLevel0 = "JPEGQLVL",
			SigEncodingXCursor = "X11CURSR",
			SigEncodingRichCursor = "RCHCURSR",
			SigEncodingPointerPos = "POINTPOS",
			SigEncodingLastRect = "LASTRECT",
			SigEncodingNewFBSize = "NEWFBSIZ";

	public final static int MaxNormalEncoding = 255;

	// Contstants used in the Hextile decoder
	public final static int HextileRaw = 1, HextileBackgroundSpecified = 2,
			HextileForegroundSpecified = 4, HextileAnySubrects = 8,
			HextileSubrectsColoured = 16;

	// Contstants used in the Tight decoder
	public final static int TightMinToCompress = 12;
	public final static int TightExplicitFilter = 0x04, TightFill = 0x08,
			TightJpeg = 0x09, TightMaxSubencoding = 0x09,
			TightFilterCopy = 0x00, TightFilterPalette = 0x01,
			TightFilterGradient = 0x02;
}
