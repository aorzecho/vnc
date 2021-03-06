//
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//  Copyright (C) 2002-2006 Constantin Kaplinsky.  All Rights Reserved.
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

package com.tigervnc.ui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.tigervnc.VncViewer;

//
// The panel which implements the user authentication scheme
//

public class AuthPanel extends Panel implements ActionListener {

  JTextField passwordField;
  JButton okButton;
  boolean AskPassword;

  //
  // Constructor.
  //

  public AuthPanel(VncViewer viewer, boolean askpassword)
  {
    AskPassword = askpassword;
    JLabel titleLabel = new JLabel("VNC Authentication", JLabel.CENTER);
    titleLabel.setFont(new Font("Helvetica", Font.BOLD, 18));

    JLabel promptLabel;
    if (AskPassword)
      promptLabel = new JLabel("Password:", JLabel.CENTER);
    else
      promptLabel = new JLabel("User:", JLabel.CENTER);

    if(AskPassword){
    	passwordField = new JPasswordField(10);
    }
    else{
    	passwordField = new JTextField(10);    	
    }
    
    passwordField.setForeground(Color.black);
    passwordField.setBackground(Color.white);

    okButton = new JButton("OK");

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    setLayout(gridbag);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets = new Insets(0,0,20,0);
    gridbag.setConstraints(titleLabel,gbc);
    add(titleLabel);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(0,0,0,0);
    gridbag.setConstraints(promptLabel,gbc);
    add(promptLabel);

    gridbag.setConstraints(passwordField,gbc);
    add(passwordField);
    passwordField.addActionListener(this);

    // gbc.ipady = 10;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(0,20,0,0);
    gbc.ipadx = 30;
    gridbag.setConstraints(okButton,gbc);
    add(okButton);
    okButton.addActionListener(this);
  }

  //
  // Move keyboard focus to the default object, that is, the password
  // text field.
  //

  public void moveFocusToDefaultField()
  {
    passwordField.requestFocus();
  }

  //
  // This method is called when a button is pressed or return is
  // pressed in the password text field.
  //

  public synchronized void actionPerformed(ActionEvent evt)
  {
    if (evt.getSource() == passwordField || evt.getSource() == okButton) {
      passwordField.setEnabled(false);
      notify();
    }
  }

  //
  // Wait for user entering a password, and return it as String.
  //

  public synchronized String getPassword() throws Exception
  {
    try {
      wait();
    } catch (InterruptedException e) { }
    return passwordField.getText();
  }

}
