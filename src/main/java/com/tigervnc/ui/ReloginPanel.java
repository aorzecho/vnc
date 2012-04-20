//
//  Copyright (C) 2002 Cendio Systems.  All Rights Reserved.
//  Copyright (C) 2002 Constantin Kaplinsky.  All Rights Reserved.
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
// ReloginPanel class implements panel with a button for logging in again,
// after fatal errors or disconnect
//
package com.tigervnc.ui;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import com.tigervnc.VncViewer;
import javax.swing.JButton;

//
// The panel which implements the Relogin button
//
public class ReloginPanel extends Panel implements ActionListener
{

    public static final String CMD_CLOSE = "close";
    public static final String CMD_RECONNECT = "reconnect";
    JButton reloginButton;
    JButton closeButton;
    VncViewer viewer;

    //
    // Constructor.
    //
    public ReloginPanel(VncViewer v)
    {
        viewer = v;
        setLayout(new FlowLayout(FlowLayout.CENTER));
        reloginButton = VncViewer.localize(new JButton("Login again"), "button.relogin");
        reloginButton.setActionCommand(CMD_RECONNECT);
        add(reloginButton);
        reloginButton.addActionListener(this);

        closeButton = VncViewer.localize(new JButton("Close window"), "button.closeWin");
        closeButton.setActionCommand(CMD_CLOSE);
        add(closeButton);
        closeButton.addActionListener(this);

    }

    //
    // This method is called when a button is pressed.
    //
    public synchronized void actionPerformed(ActionEvent evt)
    {
        if (CMD_RECONNECT == evt.getActionCommand()) {
            viewer.disconnect();
            viewer.connect();
        } else if (CMD_CLOSE == evt.getActionCommand()) {
            viewer.close();
        }
    }
}
