/*
 * Copyright (c) 2012 ProfitBricks GmbH. All Rights Reserved.
 */
package com.tigervnc.rfb.message;

import com.tigervnc.log.VncLogger;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.*;
import static org.junit.Assert.*;
import static java.awt.event.KeyEvent.*;


/**
 * Unit tests for mapping keyboard events and virtual codes.
 * 
 * @author <a href="mailto:arkadiusz.orzechowski@profitbricks.com">Arkadiusz Orzechowski</a>
 */
public class KeyboardEventMapTest {
	
	@BeforeClass
	public static void initKbMap () {
		try {
			VncLogger.ASSERT_NO_ERRORS = true;
			KeyboardEventMap.init(null);
		}  catch (IllegalStateException ignore) {}
	}

	@Test
	public void testKeyEntryConstructors() {
		for (KeyEntry ke : new KeyEntry[] {
			new KeyEntry(VK_A, 'A', ALL_MOD),
			new KeyEntry(new KeyEvent(dummy, KEY_PRESSED, 0, ALL_MOD, VK_A, 'A')),
			new KeyEntry("VK_A|65+ALT+ALT_GRAPH+CTRL+SHIFT+META")
			}) {
			assertEquals(ke.keycode, VK_A);
			assertEquals(ke.keysym, 'A');
			assertTrue("Alt is missing", ke.alt);
			assertTrue("AltGr is missing", ke.altGr);
			assertTrue("Ctrl is missing", ke.ctrl);
			assertTrue("Shift is missing", ke.shift);
			assertTrue("Meta is missing", ke.meta);
		}
	}

	@Test
	public void testJava2rfb() throws Exception {

		// load keymaps csv, check names, duplicates and min number of mappings
		int MIN_COUNT = 113;
		InputStream in = KeyboardEventMap.class.getResourceAsStream("keymaps.csv");
		SortedSet<String> mapped = new TreeSet<String>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line; // java VK_ is in column 15
			Pattern linePattern = Pattern.compile("^(?:[^,]*,){14}\\s*(VK_[A-Z_0-9]+)\\s*$");
			while ((line = reader.readLine()) != null) {
				Matcher m = linePattern.matcher(line);
				if (m.matches())
					assertTrue("Found duplicate mapping in line: " + line, 
							mapped.add( KeyEvent.class.getField(m.group(1)).getName() ));
			}
			assertTrue("Regression - number of key mapped found in keymaps.csv is " + mapped.size() + " <" + MIN_COUNT,
					mapped.size() >= MIN_COUNT);
		} finally {
			try {
				in.close();
			} catch (IOException ignore) {}
		}

		// iterate through all KeyEvent VK_* Fields and find all the key names that are properly mapped
		SortedSet<String> mappedFound = new TreeSet<String>();
		for (Field field : KeyEvent.class.getFields())
			if ( field.getName().startsWith("VK_") && (KeyboardEventMap.java2rfb(field.getInt(null)) != null) )
					mappedFound.add(field.getName());
		
		if (mapped.size() < mappedFound.size()) {
			mappedFound.retainAll(mapped); //ok, as there can be several VK_ mapping to the same value, see VK_SEPARATOR=VK_SEPARATER
		}

		if (mapped.size() > mappedFound.size()) {
			mapped.removeAll(mappedFound);
			throw new Exception("Mappings not found for " + mapped);
		} 
		
		assertArrayEquals("Mismatch in mappings found in csv file vs sucesfully mapped by KeyboardEventMap.java2rfb",
				mapped.toArray(), mappedFound.toArray());
	}

	// ================  remapping events  =================
	@Test
	public void testRemapEventGeneral() {
		
		//KEY_RELEASED:VK_B=KEY_PRESSED:VK_SHIFT,KEY_RELEASED:VK_B|66+SHIFT,KEY_RELEASED:VK_SHIFT

		assertArrayEquals(
				expEvt(new int[][] {
					{KEY_PRESSED, VK_SHIFT, CHAR_UNDEFINED, 0},
					{KEY_PRESSED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_SHIFT, CHAR_UNDEFINED, 0},
				}),
				mapEvt(KEY_RELEASED, VK_B, 'B', 0).toArray());

		assertArrayEquals(
				expEvt(new int[][] {
					{KEY_PRESSED, VK_SHIFT, CHAR_UNDEFINED, 0},
					{KEY_PRESSED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_SHIFT, CHAR_UNDEFINED, 0},
				}),
				mapEvt(KEY_RELEASED, VK_B, 'A', 0).toArray());
		
		assertArrayEquals(
				expEvt(new int[][] {
					{KEY_PRESSED, VK_SHIFT, CHAR_UNDEFINED, 0},
					{KEY_PRESSED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_SHIFT, CHAR_UNDEFINED, 0},
				}),
				mapEvt(KEY_RELEASED, VK_B, 'A', ALT_DOWN_MASK | CTRL_DOWN_MASK).toArray());
		
		assertArrayEquals(
				expEvt(new int[][] {
					{KEY_PRESSED, VK_SHIFT, CHAR_UNDEFINED, 0},
					{KEY_PRESSED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_B, 'B', SHIFT_DOWN_MASK},
					{KEY_RELEASED, VK_SHIFT, CHAR_UNDEFINED, 0},
				}),
				mapEvt(KEY_RELEASED, VK_B, CHAR_UNDEFINED, ALL_MOD).toArray());
		
	}

	@Test
	public void testRemapEventExact() {

		//KEY_PRESSED:VK_A|65+ALT+CTRL=KEY_PRESSED:VK_SHIFT,KEY_PRESSED:VK_EQUALS,KEY_RELEASED:VK_EQUALS,KEY_RELEASED:VK_SHIFT,KEY_RELEASED:VK_A|65
		
		assertArrayEquals(
				expEvt(new int[][] {
					{KEY_PRESSED, VK_SHIFT, CHAR_UNDEFINED, 0},
					{KEY_PRESSED, VK_EQUALS, CHAR_UNDEFINED, 0},
					{KEY_RELEASED, VK_EQUALS, CHAR_UNDEFINED, 0},
					{KEY_RELEASED, VK_SHIFT, CHAR_UNDEFINED, 0},
					{KEY_RELEASED, VK_A, 'A', 0},
				}),
				mapEvt(KEY_PRESSED, VK_A, 'A', ALT_DOWN_MASK | CTRL_DOWN_MASK).toArray());
		
		assertNull(mapEvt(KEY_PRESSED, VK_A, 'A', ALT_DOWN_MASK));
		assertNull(mapEvt(KEY_PRESSED, VK_A, 'A', CTRL_DOWN_MASK));
		assertNull(mapEvt(KEY_PRESSED, VK_A, 'B', ALT_DOWN_MASK | CTRL_DOWN_MASK));
		assertNull(mapEvt(KEY_PRESSED, VK_A, CHAR_UNDEFINED, ALT_DOWN_MASK | CTRL_DOWN_MASK));
		assertNull(mapEvt(KEY_PRESSED, VK_B, 'A', ALT_DOWN_MASK | CTRL_DOWN_MASK));
		assertNull(mapEvt(KEY_RELEASED, VK_A, 'A', ALT_DOWN_MASK | CTRL_DOWN_MASK));
		assertNull(mapEvt(KEY_TYPED, VK_UNDEFINED, 'A', ALT_DOWN_MASK | CTRL_DOWN_MASK));
		
	}

	// ================  remapping codes  =================
	@Test
	public void testRemapCodesGeneral() {
		
		//# keycode, whatever modifier, whatever char
		//VK_0=VK_Z
		assertEquals(	expKey(VK_Z, '0', 0), 
						mapKey(VK_0, '0', 0));

		assertEquals(	expKey(VK_Z, CHAR_UNDEFINED, 0), 
						mapKey(VK_0, CHAR_UNDEFINED, 0));
		
		assertEquals(	expKey(VK_Z, CHAR_UNDEFINED, ALL_MOD), 
						mapKey(VK_0, CHAR_UNDEFINED, ALL_MOD));
	}
	
	@Test
	public void testRemapCodesWithModifiers() {
		
		//# keycodes with modifier, whatever char
		//VK_1=VK_A
		assertEquals(	expKey(VK_A, 'a', 0), 
						mapKey(VK_1, 'a', 0));

		assertEquals(	expKey(VK_A, CHAR_UNDEFINED, 0), 
						mapKey(VK_1, CHAR_UNDEFINED, 0));

		assertEquals(	expKey(VK_A, 'a', ALL_MOD), 
						mapKey(VK_1, 'a', ALL_MOD));

		//VK_1+SHIFT=VK_B
		assertEquals(	expKey(VK_B, 'a', SHIFT_DOWN_MASK), 
						mapKey(VK_1, 'a', SHIFT_DOWN_MASK));

		//VK_1+ALT_GRAPH=C
		assertEquals(	expKey(VK_C, 'a', ALT_GRAPH_DOWN_MASK), 
						mapKey(VK_1, 'a', ALT_GRAPH_DOWN_MASK));

		//VK_1+ALT+CTRL=VK_D
		assertEquals(	expKey(VK_D, 'a', ALT_DOWN_MASK | CTRL_DOWN_MASK), 
						mapKey(VK_1, 'a', ALT_DOWN_MASK | CTRL_DOWN_MASK));

		//VK_1+SHIFT+ALT=VK_E
		assertEquals(	expKey(VK_E, 'a', ALT_DOWN_MASK | SHIFT_DOWN_MASK), 
						mapKey(VK_1, 'a', ALT_DOWN_MASK | SHIFT_DOWN_MASK));

	}
	
	@Test
	public void testRemapCodesWithModifierAndChar() {
		
		
		//# keycodes with modifier and char
		//VK_1|65=VK_M
		assertEquals(	expKey(VK_M, 'A', 0), 
						mapKey(VK_1, 'A', 0));

		assertEquals(	expKey(VK_M, 'A', ALL_MOD), 
						mapKey(VK_1, 'A', ALL_MOD));

		//VK_1|65+SHIFT=VK_N
		assertEquals(	expKey(VK_N, 'A', SHIFT_DOWN_MASK), 
						mapKey(VK_1, 'A', SHIFT_DOWN_MASK));

		//VK_1|65+ALT_GRAPH=O
		assertEquals(	expKey(VK_O, 'A', ALT_GRAPH_DOWN_MASK), 
						mapKey(VK_1, 'A', ALT_GRAPH_DOWN_MASK));

		//VK_1|65+ALT+CTRL=VK_P
		assertEquals(	expKey(VK_P, 'A', ALT_DOWN_MASK | CTRL_DOWN_MASK), 
						mapKey(VK_1, 'A', ALT_DOWN_MASK | CTRL_DOWN_MASK));

		//VK_1|66=VK_C|67
		assertEquals(	expKey(VK_C, 'C', 0), 
						mapKey(VK_1, 'B', 0));

		//VK_1|66+SHIFT=VK_D|68+ALT
		assertEquals(	expKey(VK_D, 'D', ALT_DOWN_MASK), 
						mapKey(VK_1, 'B', SHIFT_DOWN_MASK));

	}
	
	@Test
	public void testRemapCodesUndefined() {
		
		//# undefined keycode
		//VK_UNDEFINED|66=VK_B
		assertEquals(	expKey(VK_B, 'B', 0), 
						mapKey(VK_UNDEFINED, 'B', 0));

		assertEquals(	expKey(VK_B, 'B', ALL_MOD), 
						mapKey(VK_UNDEFINED, 'B', ALL_MOD));

		//VK_UNDEFINED|66+SHIFT=VK_C
		assertEquals(	expKey(VK_C, 'B', SHIFT_DOWN_MASK), 
						mapKey(VK_UNDEFINED, 'B', SHIFT_DOWN_MASK));

		//VK_UNDEFINED|66+ALT_GRAPH=VK_D
		assertEquals(	expKey(VK_D, 'B', ALT_GRAPH_DOWN_MASK), 
						mapKey(VK_UNDEFINED, 'B', ALT_GRAPH_DOWN_MASK));

		//VK_UNDEFINED|66+ALT+CTRL=VK_E
		assertEquals(	expKey(VK_E, 'B', ALT_DOWN_MASK | CTRL_DOWN_MASK), 
						mapKey(VK_UNDEFINED, 'B', ALT_DOWN_MASK | CTRL_DOWN_MASK));

		//VK_UNDEFINED|66+META=VK_F|67+CTRL+ALT
		assertEquals(	expKey(VK_F, 'C', ALT_DOWN_MASK | CTRL_DOWN_MASK), 
						mapKey(VK_UNDEFINED, 'B', META_DOWN_MASK));
	}
	
	@Test
	public void testRemapCodesExact() {
		
		//# test exact match only
		//VK_2+CTRL+ALT=VK_A
		assertEquals(	expKey(VK_A, 'B', ALT_DOWN_MASK | CTRL_DOWN_MASK), 
						mapKey(VK_2, 'B', ALT_DOWN_MASK | CTRL_DOWN_MASK));
		
		assertEquals(	expKey(VK_A, 'C', ALT_DOWN_MASK | CTRL_DOWN_MASK), 
						mapKey(VK_2, 'C', ALT_DOWN_MASK | CTRL_DOWN_MASK));
		
		assertNotMapped(VK_2, 'B', ALT_DOWN_MASK | CTRL_DOWN_MASK | ALT_GRAPH_DOWN_MASK);
		assertNotMapped(VK_2, 'B', 0);
		assertNotMapped(VK_2, 'B', CHAR_UNDEFINED);
		assertNotMapped(VK_2, 'B', ALT_DOWN_MASK);
		
		//VK_3|65=VK_A
		assertEquals(	expKey(VK_A, 'A', 0), 
						mapKey(VK_3, 'A', 0));
		
		assertEquals(	expKey(VK_A, 'A', SHIFT_DOWN_MASK), 
						mapKey(VK_3, 'A', SHIFT_DOWN_MASK));

		assertNotMapped(VK_3, 'B', 0);

		//VK_4|66+SHIFT=VK_B
		assertEquals(	expKey(VK_B, 'B', SHIFT_DOWN_MASK), 
						mapKey(VK_4, 'B', SHIFT_DOWN_MASK));
		
		assertNotMapped(VK_4, 'B', 0);
		assertNotMapped(VK_4, 'B', SHIFT_DOWN_MASK | ALT_DOWN_MASK);
		assertNotMapped(VK_4, 'B', ALT_DOWN_MASK);
		assertNotMapped(VK_4, 'C', SHIFT_DOWN_MASK);
	}

// =============== priv/helper stuff =================================
	
	private static int ALL_MOD =  ALT_DOWN_MASK 
								| ALT_GRAPH_DOWN_MASK
								| CTRL_DOWN_MASK 
								| SHIFT_DOWN_MASK 
								| META_DOWN_MASK;

	private KeyboardEventMap testKBMapper = new KeyboardEventMap(null, "keyboardfix/test_fix.properties");

	private Canvas dummy = new Canvas();
	
	private KeyEntry mapKey(int keycode, int keysym, int modifiers) {
		return testKBMapper.remapCodes(
				new KeyEvent(dummy, KEY_PRESSED, 0, modifiers, keycode, (char) keysym));
	}
	
	private KeyEntry expKey (int keycode, int keysym, int modifiers) {
		return new KeyEntry(keycode, keysym, modifiers);
	}
	
	private List<KeyboardEventMap.EvtEntry> mapEvt(int id, int keycode, int keysym, int modifiers) {
		return testKBMapper.remapEvent(
				new KeyEvent(dummy, id, 0, modifiers, keycode, (char) keysym));
	}
	
	private KeyboardEventMap.EvtEntry[] expEvt (int[] ... events) {
		List<KeyboardEventMap.EvtEntry> expected = new ArrayList<KeyboardEventMap.EvtEntry>();
		for (int[] evt : events)
			expected.add(new KeyboardEventMap.EvtEntry(evt[0], new KeyEntry(evt[1], evt[2], evt[3])));
		return expected.toArray(new KeyboardEventMap.EvtEntry[expected.size()]);
	}
	
	private void assertNotMapped (int keycode, int keysym, int modifiers) {
		assertEquals("This should not be mapped", 
				expKey(keycode, keysym, modifiers),
				mapKey(keycode, keysym, modifiers)
				);
	}


}
