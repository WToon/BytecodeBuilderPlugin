package com.guardsquare.bytecodebuilder.ui;

import javax.swing.*;
import java.awt.event.ActionListener;

public class InputPanel
extends      JPanel
{
    public JTextField   inputField      = new JTextField();

    public InputPanel() {}

    public void registerActionListener(ActionListener actionListener)
    {
        inputField.addActionListener(actionListener);
    }
}
