package com.guardsquare.bytecodebuilder;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BytecodeBuilderToolWindowFactory
implements   ToolWindowFactory
{
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        Content content = ContentFactory.getInstance().createContent(
                new BytecodeBuilderToolWindowContent().contentPanel, "BytecodeBuilder", false);
        toolWindow.getContentManager().addContent(content);
    }


    private static class BytecodeBuilderToolWindowContent
    {
        public JPanel       contentPanel    = new JPanel();
        public JTextField   inputField      = new JTextField();
        public JTextArea    outputField     = new JTextArea();

        public String inputSnippet;

        public BytecodeBuilderToolWindowContent()
        {
            inputField.addActionListener(
                    new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            inputSnippet = inputField.getText();
                            outputField.setText(inputSnippet);
                        }
                    });

            contentPanel.setLayout(new GridLayout(2,1));
            
            contentPanel.add(inputField);
            contentPanel.add(outputField);
        }
    }
}
