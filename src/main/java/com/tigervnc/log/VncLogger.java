/*
 * Copyright (c) 2012 ProfitBricks GmbH. All Rights Reserved.
 */
package com.tigervnc.log;

import java.util.logging.Level;

/**
 *
 * @author <a href="mailto:arkadiusz.orzechowski@profitbricks.com">Arkadiusz Orzechowski</a>
 */
public class VncLogger {

	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getAnonymousLogger();
	private static Level level = Level.INFO;

	private final String name;
	
	public static boolean ASSERT_NO_ERRORS = false; //used in unit tests to fail on errors/warnings
	
	public static void setDefaultLevel (Level level) {
		VncLogger.level = level;
		logger.setLevel(level);
		logger.info("level set: " + level);
	}
	
	public static VncLogger getLogger(Class clazz) {
		return new VncLogger(clazz.getName());
	}
	
	private VncLogger(String name) {
		this.name = name;
	}

	//========= logging ======
	
	public void debug(Object message) {
		if (level.intValue() <= Level.FINE.intValue())
			logger.log(Level.INFO, formatMsg(message));
	}

	public void info(Object message) {
		if (level.intValue() <= Level.INFO.intValue())
			logger.log(Level.INFO, formatMsg(message));
	}

	public void warn(Object message, Throwable t) {
		if (ASSERT_NO_ERRORS)
			throw new AssertionError(message + t.toString());
		if (level.intValue() <= Level.WARNING.intValue())
			logger.log(Level.WARNING, formatMsg(message), t);
	}
	public void warn(Object message) {
		if (ASSERT_NO_ERRORS)
			throw new AssertionError(message);
		if (level.intValue() <= Level.WARNING.intValue())
			logger.log(Level.WARNING, formatMsg(message));
	}

	public void error(Object message) {
		if (ASSERT_NO_ERRORS)
			throw new AssertionError(message);
		logger.log(Level.SEVERE, formatMsg(message));
	}

	public void error(Object message, Throwable t) {
		if (ASSERT_NO_ERRORS)
			throw new AssertionError(message + t.toString());
		logger.log(Level.SEVERE, formatMsg(message), t);
	}


// ============= priv =======================

	private String formatMsg (Object msg) {
		return String.format("%s: %s", new Object[] {
			name, msg
		});
	}
}
