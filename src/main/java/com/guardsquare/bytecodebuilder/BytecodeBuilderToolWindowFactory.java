package com.guardsquare.bytecodebuilder;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;

public class BytecodeBuilderToolWindowFactory
implements   ToolWindowFactory
{
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        Content content = ContentFactory.getInstance().createContent(
                new BytecodeBuilderToolWindowContent().contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }


    private static class BytecodeBuilderToolWindowContent
    {
        public JPanel       contentPanel    = new JPanel();
        public JTextArea    inputField      = new JTextArea();
        public JTextArea    outputField     = new JTextArea();

        public BytecodeBuilderToolWindowContent()
        {
            setupInputPanel();

            contentPanel.setLayout(new GridLayout(2,1));
            
            contentPanel.add(inputField);
            contentPanel.add(outputField);
        }


        /**
         * Sets up the input panel.
         */
        private void setupInputPanel()
        {
            Document inputFieldDocument = inputField.getDocument();
            inputFieldDocument.addDocumentListener(
                    new DocumentListener()
                    {
                        @Override
                        public void changedUpdate(DocumentEvent e) { /* Never triggered by JTextAre. */ }

                        @Override
                        public void insertUpdate(DocumentEvent e)
                        {
                            outputField.setText(inputField.getText());
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e)
                        {
                            outputField.setText(inputField.getText());
                        }
                    });
        }
    }
}
