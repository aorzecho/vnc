/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tigervnc.rfb.message;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.log4j.Logger;

/**
 *
 * @author aorzecho
 */
public class ApplyKbFixAction implements Action, ChangeListener {

	public static Logger logger = Logger.getLogger(ApplyKbFixAction.class);
	final KeyboardEventMap.KbFix fix;
	private Map props;
	Set<PropertyChangeListener> changeListeners;

	public ApplyKbFixAction(Map props, KeyboardEventMap.KbFix fix) {
		this.fix = fix;
		this.props = props;
		changeListeners = new HashSet<PropertyChangeListener>();
	}

	private void notifyListeners(PropertyChangeEvent evt) {
		for (PropertyChangeListener listener : changeListeners) {
			listener.propertyChange(evt);
		}
	}

	@Override
	public Object getValue(String key) {
		return props.get(key);
	}

	@Override
	public void putValue(String key, Object value) {
		Object oldValue = props.get(key);
		props.put(key, value);
		notifyListeners(new PropertyChangeEvent(this, key, oldValue, value));
	}

	@Override
	public void setEnabled(boolean enabled) {
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeListeners.remove(listener);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update(AbstractButton.class.cast(e.getSource()).isSelected());
	}

	public boolean isApplied() {
		return KeyboardEventMap.getInstance().isApplied(fix);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		update(AbstractButton.class.cast(e.getSource()).isSelected());
	}

	private void update(boolean selected) {
		if (selected && !KeyboardEventMap.getInstance().isApplied(fix)) {
			KeyboardEventMap.getInstance().applyFix(fix);
			logger.info("enabled " + props.get(NAME));
		} else if (!selected && KeyboardEventMap.getInstance().isApplied(fix)) {
			KeyboardEventMap.getInstance().unapplyFix(fix);
			logger.info("disabled " + props.get(NAME));
		}
	}
}
