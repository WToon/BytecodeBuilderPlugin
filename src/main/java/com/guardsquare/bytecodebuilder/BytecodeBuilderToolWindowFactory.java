package com.guardsquare.bytecodebuilder;


import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class BytecodeBuilderToolWindowFactory
implements   ToolWindowFactory, DumbAware
{
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        Content content = ContentFactory.getInstance().createContent(
                new BytecodeBuilderToolWindow(project).contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
