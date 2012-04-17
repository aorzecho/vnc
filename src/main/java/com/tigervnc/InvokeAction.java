/*
 * Copyright (c) 2012 ProfitBricks GmbH. All Rights Reserved.
 */
package com.tigervnc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.tigervnc.log.VncLogger;

/**
 *
 * @author <a href="mailto:arkadiusz.orzechowski@profitbricks.com">Arkadiusz Orzechowski</a>
 */
public class InvokeAction implements ActionListener {

    private final Method method;
    private final Object obj;
    private final Object[] args;
	public static VncLogger logger = VncLogger.getLogger(InvokeAction.class);
    
    public InvokeAction (Class clazz, String methodName, Object ... args) throws NoSuchMethodException {
        Class[] paramTypes = new Class[args.length];
        for (int i=0; i<args.length; i++)
            paramTypes[i] = args[i].getClass();
        this.method = clazz.getMethod(methodName, paramTypes);
        this.obj = null;
        this.args = args;
    }

    public InvokeAction (Object obj, String methodName, Object ... args) throws NoSuchMethodException {
        Class[] paramTypes = new Class[args.length];
        for (int i=0; i<args.length; i++)
            paramTypes[i] = args[i].getClass();
        this.method = obj.getClass().getMethod(methodName, paramTypes);
        this.obj = obj;
        this.args = args;
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
