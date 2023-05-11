package com.guardsquare.bytecodebuilder;


import com.guardsquare.bytecodebuilder.backend.CodeUtil;
import com.intellij.lang.Language;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
            "    public static void " + METHOD_NAME + "(String... args) {\n" +
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

    private static class CustomLanguageTextField extends LanguageTextField {
        public CustomLanguageTextField(Language language, Project project, String text, boolean oneLineMode) {
            super(language, project, text, oneLineMode);
        }

        @Override
        protected @NotNull EditorEx createEditor() {
            EditorEx editor = super.createEditor();
            editor.getSettings().setLineNumbersShown(true);
            editor.getSettings().setAutoCodeFoldingEnabled(true);
            editor.getSettings().setFoldingOutlineShown(true);
            editor.getSettings().setAllowSingleLogicalLineFolding(true);
            return editor;
        }
    }

    private static class BytecodeBuilderToolWindowContent
    {

        public  JPanel                   contentPanel               = new JPanel();
        public  LanguageTextField        inputField;
        public  JTextArea                outputText                 = new JTextArea();
        public  JScrollPane              outputField                = new JBScrollPane(outputText);
        public  JButton                  classPathChooserOpenButton = new JButton("Set classpath");
        public  JLabel                   classPathLabel             = new JLabel("No custom classpath set.");
        public  JFileChooser             classPathFileChooser       = new JFileChooser();
        public  String                   customClassPath            = "";
        private ScheduledExecutorService executorService            = Executors.newSingleThreadScheduledExecutor();
        private ScheduledFuture<?>       updateFuture               = null;

        public BytecodeBuilderToolWindowContent(Project project)
        {
            inputField = new CustomLanguageTextField(StdLanguages.JAVA, project, PROMPT, false);

            // Set up input panel.
            setupInputPanel();

            // Set up file chooser button etc.
            setUpClasspathChooser();

            // Set up main panel.
            contentPanel.setLayout(new GridLayout(2,1));
            contentPanel.add(inputField);

            // Construct the lower layout pane
            setUpLowerLayoutPane();
        }

        private void setUpClasspathChooser()
        {
            classPathFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            classPathChooserOpenButton.addActionListener(e -> {
                int resultCode = classPathFileChooser.showOpenDialog(null);
                if (resultCode == JFileChooser.APPROVE_OPTION)
                {
                    customClassPath = classPathFileChooser.getSelectedFile().getAbsolutePath();
                    classPathLabel.setText(customClassPath);
                }
            });
        }

        private void setUpLowerLayoutPane()
        {
            JPanel containerPanel = new JPanel(new GridBagLayout());
            JPanel containerPanel2 = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0.8;
            c.fill = GridBagConstraints.HORIZONTAL;
            containerPanel2.add(classPathLabel, c);
            c.gridx = 1;
            c.weightx = 0.2;
            containerPanel2.add(classPathChooserOpenButton, c);
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 0.8;
            c.fill = GridBagConstraints.BOTH;
            containerPanel.add(outputField, c);
            c.gridy = 1;
            c.weighty = 0.2;
            containerPanel.add(containerPanel2, c);
            contentPanel.add(containerPanel);
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
                            if (updateFuture != null)
                            {
                                updateFuture.cancel(false);
                            }
                            updateFuture = executorService.schedule(() -> {
                                updateOutputPanel();
                                updateFuture = null;
                            }, 500L, TimeUnit.MILLISECONDS);
                        }
                    }
            );
        }

        /**
         * Update the output panel.
         */
        private void updateOutputPanel()
        {
            outputText.setText(CodeUtil.getProGuardInstructions(inputField.getText(), customClassPath));
        }
    }
}
