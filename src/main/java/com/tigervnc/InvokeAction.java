/*
 * Copyright (c) 2012 ProfitBricks GmbH. All Rights Reserved.
 */
package com.tigervnc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import com.tigervnc.log.VncLogger;

/**
 *
 * @author <a href="mailto:arkadiusz.orzechowski@profitbricks.com">Arkadiusz Orzechowski</a>
 */
public class InvokeAction implements ActionListener {

	public static VncLogger logger = VncLogger.getLogger(InvokeAction.class);
    
	private final Method method;
    private final Object[] args;
    private Object obj;
    
    public InvokeAction (Class clazz, String methodName, Object ... args) throws NoSuchMethodException {
        Class[] paramTypes = new Class[args.length];
        for (int i=0; i<args.length; i++)
            paramTypes[i] = args[i].getClass();
		this.method = clazz.getMethod(methodName, paramTypes);
		this.args = args;
    }

    public InvokeAction (Object obj, String methodName, Object ... args) throws NoSuchMethodException {
		this(obj.getClass(), methodName, args);
        this.obj = obj;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        try {
            method.invoke(obj, args);
        } catch (Exception ex) {
            logger.error("Exception performing invoke action", ex);
        }
    }
}
