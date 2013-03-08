package com.tigervnc.rfb.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;

import com.tigervnc.rfb.Encodings;


public class SetEncodingsTest {

	@Test
	public void test_getSetEncodings(){
		ArrayList<Integer> encs = new ArrayList<Integer>();
		encs.add(Encodings.ENCODING_EXTENDED_KEY_EVENT);
		byte[] set_encs = new SetEncodings(encs).getBytes();		
		Assert.assertEquals(2, Encodings.SET_ENCODINGS);
		
//     		=============== ==================== ========== =======================
//			No. of bytes    Type                 [Value]    Description
//			=============== ==================== ========== =======================
//			1               ``U8``               2          *message-type*
//			1                                               *padding*
//			2               ``U16``                         *number-of-encodings*
//			=============== ==================== ========== =======================
//
		
		Assert.assertEquals(Encodings.SET_ENCODINGS, set_encs[0]);
		Assert.assertEquals(0, set_encs[1]);
		ByteBuffer bb = ByteBuffer.wrap(new byte[]{set_encs[2], set_encs[3]});
		Assert.assertEquals(1, bb.asShortBuffer().get());
		
//			followed by *number-of-encodings* repetitions of the following:
//
//			=============== =============================== =======================
//			No. of bytes    Type                            Description
//			=============== =============================== =======================
//			4               ``S32``                         *encoding-type*
//			=============== =============================== =======================
		bb = ByteBuffer.wrap(new byte[]{set_encs[4], set_encs[5], set_encs[6], set_encs[7]});
		Assert.assertEquals(Encodings.ENCODING_EXTENDED_KEY_EVENT, bb.asIntBuffer().get());
	}
}
