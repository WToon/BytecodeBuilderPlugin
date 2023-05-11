package com.guardsquare.bytecodebuilder;


import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
        public JPanel          contentPanel    = new JPanel();
        public EditorTextField inputField      = new EditorTextField();
        public EditorTextField outputField     = new EditorTextField();

        public BytecodeBuilderToolWindowContent()
        {
            // Set up input panel.
            setupInputPanel();

            // Set up main panel.
            contentPanel.setLayout(new GridLayout(2,1));
            contentPanel.add(inputField);
            contentPanel.add(outputField);
        }


        /**
         * Sets up the input panel.
         */
        private void setupInputPanel()
        {
            inputField.setFont(Font.getFont(Font.DIALOG_INPUT));
            inputField.addDocumentListener(
                    new DocumentListener() {
                        @Override
                        public void documentChanged(@NotNull DocumentEvent event) {
                            updateOutputPanel();
                        }
                    }
            );
        }


        /**
         * Update the output panel.
         */
        private void updateOutputPanel()
        {
            outputField.setText(inputField.getText());
        }
    }
}
