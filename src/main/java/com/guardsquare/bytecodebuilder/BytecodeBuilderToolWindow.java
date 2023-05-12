package com.guardsquare.bytecodebuilder;

import com.guardsquare.bytecodebuilder.backend.CodeUtil;
import com.intellij.lang.Language;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class BytecodeBuilderToolWindow
{
    public static final  String CLASS_NAME  = "Container";
    public static final  String METHOD_NAME = "main";
    private static final String PROMPT      =
            "class " + CLASS_NAME + " \n{\n" +
            "    public static void " + METHOD_NAME + "(String... args) \n" +
            "    {\n" +
            "        // Put your code here.\n" +
            "        \n" +
            "    }\n" +
            "}\n";


    public JPanel                           contentPanel               = new JPanel();
    public LanguageTextField                inputField;
    public JTextArea                        outputText                 = new JTextArea();
    public JScrollPane                      outputField;
    public JButton                          copyCodeButton             = new JButton("Copy code");
    public JButton                          classPathChooserOpenButton = new JButton("Set classpath");
    public JFileChooser                     classPathFileChooser       = new JFileChooser();
    public JLabel                           classPathLabel             = new JLabel("No custom classpath set.");
    public String                           customClassPath            = "";
    private ScheduledFuture<?>              updateFuture               = null;
    private final ScheduledExecutorService  executorService      = Executors.newSingleThreadScheduledExecutor();
    private final Project                   project;


    public BytecodeBuilderToolWindow(Project project)
    {
        this.project = project;

        // Set up input panel.
        setupInputPanel();

        // Set up output panel.
        setupOutputPanel();

        // Set up the copy code button.
        setUpCopyCodeButton();

        // Set up file chooser button etc.
        setUpClasspathChooser();

        // Set up main panel.
        contentPanel.setLayout(new GridLayout(2,1));
        contentPanel.add(inputField);

        // Construct the lower layout pane
        setUpLowerLayoutPane();
    }

    // Helper methods.

    private void setupInputPanel()
    {
        inputField = new CustomLanguageTextField(StdLanguages.JAVA, project, PROMPT, false);
        inputField.addDocumentListener(
                new DocumentListener()
                {
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


    private void setupOutputPanel()
    {
        outputField = new JBScrollPane(outputText);
        // Initialize the output panel.
        updateOutputPanel();
    }


    private void setUpCopyCodeButton()
    {
        copyCodeButton.addActionListener(e -> copyCodeToClipBoard());
    }


    private void setUpClasspathChooser()
    {
        classPathFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        classPathChooserOpenButton.addActionListener(e -> {
            int resultCode = classPathFileChooser.showOpenDialog(null);
            if (resultCode == JFileChooser.APPROVE_OPTION)
            {
                customClassPath = classPathFileChooser.getSelectedFile().getAbsolutePath();
                classPathLabel.setText("Using custom class path: " + customClassPath);
            }
        });
    }


    private void setUpLowerLayoutPane()
    {
        JPanel containerPanel = new JPanel(new GridBagLayout());
        JPanel containerPanel2 = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.2;
        c.fill = GridBagConstraints.HORIZONTAL;
        containerPanel2.add(copyCodeButton, c);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.8;
        containerPanel2.add(classPathLabel, c);
        c.gridx = 1;
        c.gridy = 1;
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


    private void updateOutputPanel()
    {
        outputText.setText(CodeUtil.getProGuardInstructions(inputField.getText(), customClassPath));
    }


    private void copyCodeToClipBoard() {
        CopyPasteManager.getInstance().setContents(new StringSelection(outputText.getText()));
    }


    // Inner classes.

    private static class CustomLanguageTextField extends LanguageTextField
    {
        public CustomLanguageTextField(Language language, Project project, String text, boolean oneLineMode)
        {
            super(language, project, text, oneLineMode);;
        }

        @Override
        protected @NotNull EditorEx createEditor()
        {
            EditorEx       editor   = super.createEditor();
            EditorSettings settings = editor.getSettings();

            // 9 for the newline + indentation.
            editor.getCaretModel().moveToOffset(PROMPT.indexOf("// Put your code here.") + "// Put your code here.".length() + 9);

            settings.setLineNumbersShown(true);
            settings.setAutoCodeFoldingEnabled(true);
            settings.setFoldingOutlineShown(true);
            settings.setAllowSingleLogicalLineFolding(true);
            settings.setIndentGuidesShown(true);
            settings.setGutterIconsShown(true);
            settings.setTabSize(4);
            settings.setShowIntentionBulb(true);
            settings.setRefrainFromScrolling(true);
            return editor;
        }
    }
}