package com.tigervnc.rfb.message;

import com.tigervnc.Util;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.Action;
import static java.awt.event.KeyEvent.*;

public class KeyboardEventMap {

    public static final class EvtEntry {

        public final int evtId;
        public final KeyEntry key;

        public EvtEntry(int evtId, KeyEntry key) {
            this.evtId = evtId;
            this.key = key;
        }

        public EvtEntry(KeyEvent evt) {
            this.evtId = evt.getID();
            this.key = new KeyEntry(evt);
        }
        
        //  (KEY_PRESSED|KEY_RELEASED|KEY_TYPED):VK_XXX[+()]
        public EvtEntry(String value) throws Exception {
            String[] vs = value.split(":"); // type:keycode
            evtId = KeyEvent.class.getField(vs[0].trim()).getInt(null);
            key = new KeyEntry(vs[1]);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EvtEntry)) {
                return false;
            }
            EvtEntry other = EvtEntry.class.cast(obj);
            return other.evtId == this.evtId
                    && this.key.equals(other.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode() ^ (17 * evtId);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            switch (evtId) {
                case KeyEvent.KEY_PRESSED:  buf.append("KEY_PRESSED:"); break;
                case KeyEvent.KEY_TYPED:    buf.append("KEY_TYPED:");   break;
                case KeyEvent.KEY_RELEASED: buf.append("KEY_RELEASED:");break;
            }
            buf.append(key.toString());
            return buf.toString();
        }
    }

    public static final class KbFix implements Comparable<KbFix> {

        final boolean optional;
	final String group;
	final Integer priority;
        final Map<EvtEntry, List<EvtEntry>> eventRemap;
        final Map<KeyEntry, KeyEntry> codeRemap;

        public KbFix(Map<String, String> params, Map<KeyEntry, KeyEntry> codeRemap, Map<EvtEntry, List<EvtEntry>> eventRemap) {
            this.optional = Boolean.valueOf(params.get("optional"));
	    String prS = params.get("priority");
	    if (prS != null)
		    priority = Integer.valueOf(prS);
	    else 
		    priority = Integer.MIN_VALUE;
	    this.group = params.get("group") == null ? "" : params.get("group");
            this.eventRemap = eventRemap;
            this.codeRemap = codeRemap;
	    
        }
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(optional ? "\n[optional fix]" : "\n[default fix]");
		buf.append(" group:");
		buf.append(group);
		buf.append(" priority:");
		buf.append(priority);
		buf.append("\nevent remap: ");
		buf.append(eventRemap.toString());
		buf.append("\ncode remap: ");
		buf.append(codeRemap.toString());
		return buf.toString();
	}

	@Override
	public int compareTo(KbFix other) {
		int ret = other.priority.compareTo(this.priority);// reverse
		if (ret == 0)
			ret = this.group.compareTo(other.group);
		if (ret == 0)
			ret = 
				Integer.valueOf(System.identityHashCode(this))
				.compareTo(System.identityHashCode(other));//still can be wrong sometimes...
		return ret;
	}
    }

    private static final Logger logger = Logger.getLogger(KeyboardEventMap.class.getName());
    private static final Pattern CSV_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");
    private static final Map<Integer, Integer> javaCode2rfb = new HashMap<Integer, Integer>(512);
    private static final SortedSet<KbFix> fixes = new TreeSet<KbFix>();
    public static final Map<String, List<ApplyKbFixAction>> manualFixes = new HashMap<String, List<ApplyKbFixAction>>();
    public static final String CURRENT_OS;

    private static int xt2rfb(int x) {
//         RFB keycodes are XT kbd keycodes with a slightly
//         different encoding of 0xe0 scan codes. RFB uses
//         the high bit of the first byte, instead of the low
//         bit of the second byte.
        return ((x & 0x100) >> 1) | (x & 0x7f);
    }

    private static void loadJava2RfbKeymap() {
        InputStream in = KeyboardEventMap.class.getResourceAsStream("keymaps.csv");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line = reader.readLine(); // header line
            String[] headers = CSV_SPLIT_PATTERN.split(line);
            int xtCol = -1;
            int jvCol = -1;
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].matches("\"?XT KBD\"?")) {
                    xtCol = i;
                } else if (headers[i].matches("\"?Java\"?")) {
                    jvCol = i;
                }

            }
            if (jvCol < 0 || xtCol < 0) {
                throw new Exception("Invalid input file, 'XT KB'=" + xtCol + " 'Java'=" + jvCol);
            }
            while ((line = reader.readLine()) != null) {
                String[] codes = CSV_SPLIT_PATTERN.split(line);
                if (codes.length <= jvCol || codes.length <= xtCol) {
                    continue;
                }
                try {
                    javaCode2rfb.put(KeyEvent.class.getField(codes[jvCol].trim()).getInt(null),
                            xt2rfb(Integer.parseInt(codes[xtCol])));
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Exception reading line:  " + line, ex);
                }

            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    // Loads properties from file. Filters those not for current OS.
    private static Map<String, String> loadMap(String file) {
        logger.log(Level.INFO, "loading file: {0}", file);
        InputStream in = KeyboardEventMap.class.getResourceAsStream(file);
        Map<String, String> mappings = new LinkedHashMap<String, String>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0 || line.trim().startsWith("#")) {
                    continue;
                }
                String[] kv = line.split("=");
                if (kv.length != 2) {
                    logger.log(Level.SEVERE, "Invalid line:  {0}", line);
                    continue;
                }
                String[] os_key = kv[0].split("\\?");
                if (os_key.length == 2) {
                    logger.log(Level.INFO, "OS-specific line:  {0}", line);
                    List<String> oses = Arrays.asList(os_key[0].split(","));
                    if (oses.contains(CURRENT_OS))
                        mappings.put(os_key[1], kv[1]);
                } else if (os_key.length == 1) {
                    mappings.put(kv[0], kv[1]);
                } else {
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        logger.log(Level.INFO, "loaded:  {0}", mappings);
        return mappings;
    }

    private static Map<KeyEntry, KeyEntry> loadKeycodeRemap(String file) {
        Map<String, String> props = loadMap(file);
        Map<KeyEntry, KeyEntry> codes = new HashMap<KeyEntry, KeyEntry>(props.size());
        for (Map.Entry<String, String> prop : props.entrySet()) {
            codes.put(new KeyEntry(prop.getKey()), new KeyEntry(prop.getValue()));
        }
        logger.log(Level.INFO, "Loaded keycodeRemap {0}: {1}", new Object[] {file, codes});
        return codes;
    }

    private static Map<EvtEntry, List<EvtEntry>> loadEventRemap(String file) {
        Map<String, String> rawMap = loadMap(file);
        Map<EvtEntry, List<EvtEntry>> remap = new HashMap<EvtEntry, List<EvtEntry>>();
        for (Map.Entry<String, String> entry : rawMap.entrySet()) {
            try {
                String[] values = entry.getValue().split(",");
                List<EvtEntry> events = new ArrayList<EvtEntry>(values.length);
		for (String value : values)
			events.add(new EvtEntry(value));
                remap.put(new EvtEntry(entry.getKey()), events);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Exception mappping " + entry.getKey() + "=" + entry.getValue(), ex);
            }
        }
        logger.log(Level.INFO, "Loaded eventRemap {0}: {1}", new Object[] {file, remap});
        return remap;
    }

    private static void loadManualFixes() {
        Map<String, String> fixes = loadMap("keyboardfix/fix.properties");
        for (Map.Entry<String, String> entry : fixes.entrySet()) {
            String dir = "keyboardfix" + "/" + entry.getKey() + "/";
            logger.log(Level.INFO, "loading fix: {0} " + 
		    Locale.getDefault().getLanguage() , dir);
            Map<String, String> params =parseParams(entry.getValue());

            KbFix fix = new KbFix(
                    params,
                    loadKeycodeRemap(dir + "keycode_remap.properties"),
                    loadEventRemap(dir + "keyevent_remap.properties"));
            if (fix.optional) {
                String groupName = params.get("group");
		if (groupName == null)
			groupName = "";
                List<ApplyKbFixAction> fixGroup = manualFixes.get(groupName);
                if (fixGroup == null) {
                    fixGroup = new ArrayList<ApplyKbFixAction>();
                    manualFixes.put(groupName, fixGroup);
                }
                fixGroup.add(new ApplyKbFixAction(loadMap(dir + "desc.properties"), fix));
                if (isActive(params)) {
	            logger.log(Level.INFO, "apply fix: {0} ", fix);
                    applyFix(fix);
                }
            } else {
                applyFix(fix);
            }
        }
    }

    private static boolean isActive (Map<String, String> params) {
        boolean activeByDefault = Boolean.valueOf(params.get("activeByDefault"));
        boolean activeLang = Locale.getDefault().getLanguage().equals(
			params.get("activatorLang"));
        String activatorOS = params.get("activatorOS");
        boolean activeOS = activatorOS == null
            || Arrays.asList(activatorOS.split(",")).contains(CURRENT_OS);
        return activeByDefault 
                        || ( activeLang && activeOS );
    }
    
    // comma separated key:value,key1:value1....
    private static Map<String, String> parseParams (String paramString) {
	    Map<String, String> params = new HashMap<String, String> ();
	    for (String param : paramString.split(",")) {
		    String[] kv = param.split(":");
		    if (kv.length == 1)
			    params.put(kv[0].trim(), "true");
		    else if (kv.length == 2)
			    params.put(kv[0].trim(), kv[1].trim());
		    else
			    throw new IllegalArgumentException ("invalid param: " + param);
	    }
	    return params;
    }
    
    static {
        if (Util.isLinux())
            CURRENT_OS = "linux";
        else if (Util.isMac())
            CURRENT_OS = "mac";
        else if (Util.isWin())
            CURRENT_OS = "windows";
        else
            CURRENT_OS = "unknown";
        logger.log(Level.INFO, "CURRENT_OS: {0}", CURRENT_OS);
        loadJava2RfbKeymap();
        loadManualFixes();
    }

// ===================== mapping methods ===============================
    public static Integer java2rfb(int keycode) {
        return javaCode2rfb.get(keycode);
    }

    public static List<EvtEntry> remapEvent(KeyEvent evt) {
        EvtEntry entry = new EvtEntry(evt);
        for (KbFix fix : fixes) {
            List<EvtEntry> remappedEvt = fix.eventRemap.get(entry);
            if (remappedEvt != null) {
                logger.log(Level.INFO, "remapEvent {0} -> {1}", new Object[] {
                            entry, remappedEvt});
                return remappedEvt;
            }
        }

	//try again for keycode only mappings
        if (evt.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
		entry = new EvtEntry(evt.getID(), 
			new KeyEntry(evt.getKeyCode(), KeyEvent.CHAR_UNDEFINED, evt.getModifiersEx()));
		
		for (KbFix fix : fixes) {
			List<EvtEntry> remappedEvt = fix.eventRemap.get(entry);
			if (remappedEvt != null) {
				logger.log(Level.INFO, "remapEvent {0} -> {1}", new Object[] {
					entry, remappedEvt});
				return remappedEvt;
			}
		}
	}
        return null;
    }

    public static KeyEntry remapCodes(KeyEvent evt) {
//        KeyEntry key = new KeyEntry(evt);
	
	KeyEntry[] searchKeys = new KeyEntry[] {
		new KeyEntry(evt),
		new KeyEntry(evt.getKeyCode(), KeyEvent.CHAR_UNDEFINED, evt.getModifiersEx()),
		new KeyEntry(evt.getKeyCode(), evt.getKeyChar(), 0),
		new KeyEntry(evt.getKeyCode(), KeyEvent.CHAR_UNDEFINED, 0)
	};

        for (KeyEntry key : searchKeys) {
            for (KbFix fix : fixes) { 
                KeyEntry remappedKey = fix.codeRemap.get(key);
                if (remappedKey != null) {
                    remappedKey = new KeyEntry (
			    remappedKey.keycode, 
			    remappedKey.keysym == CHAR_UNDEFINED ? key.keysym : remappedKey.keysym, 
			    remappedKey.getModifierMask() == 0 ? evt.getModifiersEx() : remappedKey.getModifierMask());
                    logger.log(Level.INFO, "remapCode {0} -> {1}", new Object[] {
                                key, remappedKey
                            });
                    return remappedKey;
                }
            }
        }
        return searchKeys[0];
    }

//============  enable/disable fixes =========================    
    public static void applyFix(KbFix fix) {
        fixes.add(fix);
    }

    public static void unapplyFix(KbFix fix) {
        fixes.remove(fix);
    }

    public static boolean isApplied(KbFix fix) {
        return fixes.contains(fix);
    }
}