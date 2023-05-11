package com.guardsquare.bytecodebuilder.backend;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.BranchTargetFinder;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class LabelPrinter implements InstructionVisitor {
    private final PrintWriter printWriter;
    private final BranchTargetFinder targetFinder;
    private final Map<Integer, String> labelNames = new HashMap<>();

    public LabelPrinter(PrintWriter printWriter, BranchTargetFinder targetFinder) {
        this.printWriter = printWriter;
        this.targetFinder = targetFinder;
    }

    @Override
    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {
        if (this.targetFinder.isBranchTarget(offset)) {
            this.printWriter.println("Label " + getLabelName(offset) + " = composer.createLabel();");
        }
    }

    public String getLabelName(int offset) {
        String label = "label" + labelNames.size();
        if (!labelNames.containsKey(offset)) {
            labelNames.put(offset, label);
        }
        return labelNames.get(offset);
    }
}
