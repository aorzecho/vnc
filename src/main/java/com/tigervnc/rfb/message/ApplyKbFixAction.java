/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tigervnc.rfb.message;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author aorzecho
 */
public class ApplyKbFixAction implements Action, ChangeListener {

    private static final Logger logger = Logger.getLogger(ApplyKbFixAction.class.getName());
    private final KeyboardEventMap.KbFix fix;
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
        return KeyboardEventMap.isApplied(fix);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        update(AbstractButton.class.cast(e.getSource()).isSelected());
    }

    private void update(boolean selected) {
        if (selected && !KeyboardEventMap.isApplied(fix)) {
            KeyboardEventMap.applyFix(fix);
            logger.log(Level.INFO, "enabled " + props.get(NAME));
        } else if (!selected && KeyboardEventMap.isApplied(fix)) {
            KeyboardEventMap.unapplyFix(fix);
            logger.log(Level.INFO, "disabled " + props.get(NAME));
        }
    }
}
