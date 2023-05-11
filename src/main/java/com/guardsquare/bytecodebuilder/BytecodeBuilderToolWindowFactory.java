package com.guardsquare.bytecodebuilder;


import com.guardsquare.bytecodebuilder.backend.CodeUtil;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class BytecodeBuilderToolWindowFactory
implements   ToolWindowFactory, DumbAware
{
    public static final String CLASS_NAME = "Container";
    public static final String METHOD_NAME = "main";

    private static final String PROMPT =
            "class " + CLASS_NAME + " {\n" +
            "    public static void " + METHOD_NAME + "() {\n" +
            "        // Put your code here.\n" +
            "    }\n" +
            "}";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        Content content = ContentFactory.getInstance().createContent(
                new BytecodeBuilderToolWindowContent(project).contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class BytecodeBuilderToolWindowContent
    {
        public JPanel            contentPanel = new JPanel();
        public LanguageTextField inputField;
        public JTextArea         outputField  = new JTextArea();

        public BytecodeBuilderToolWindowContent(Project project)
        {
            inputField = new LanguageTextField(StdLanguages.JAVA, project, PROMPT);
            inputField.setOneLineMode(false);

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
            inputField.addDocumentListener(
                    new DocumentListener() {
                        @Override
                        public void documentChanged(@NotNull DocumentEvent event)
                        {
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
            outputField.setText(CodeUtil.getProGuardInstructions(inputField.getText()));
        }
    }
}
