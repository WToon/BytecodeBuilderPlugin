package com.guardsquare.bytecodebuilder

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class BytecodeBuilderWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(
            BytecodeBuilderToolWindowContent().content, "BytecodeBuilderTool", false)
        toolWindow.contentManager.addContent(content)
    }

    class BytecodeBuilderToolWindowContent : ActionListener {
        val content = JPanel()
        private val textField = JTextField()
        private val textDisplay = JLabel()

        init {
            content.layout = BorderLayout(5, 5)
            content.add(textField)
            textField.addActionListener(this)
        }

            override fun actionPerformed(e: ActionEvent?) {
                textDisplay.text = textField.text
            }
    }
}