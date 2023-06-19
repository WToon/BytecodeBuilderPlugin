package com.guardsquare.bytecodebuilder.backend;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.BranchTargetFinder;

import java.io.PrintWriter;

public class InstructionPrinter implements InstructionVisitor {
    private final PrintWriter            printWriter;
    private final BranchTargetFinder     targetFinder;
    private final LabelPrinter           labelPrinter;
    private final ConstantArgumentFinder constantArgumentFinder = new ConstantArgumentFinder();

    public InstructionPrinter(PrintWriter printWriter, BranchTargetFinder targetFinder, LabelPrinter labelPrinter) {
        this.printWriter = printWriter;
        this.targetFinder = targetFinder;
        this.labelPrinter = labelPrinter;
    }

    private void visitBefore(int offset) {
        if (this.targetFinder.isBranchTarget(offset)) {
            printWriter.println("        .label(" + this.labelPrinter.getLabelName(offset) + ")");
        }
    }

    @Override
    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction) {
        visitBefore(offset);
        this.printWriter.print("        ." + getName(simpleInstruction));
        this.printWriter.print("(");
        if (simpleInstruction.constant > 5) {
            this.printWriter.print(simpleInstruction.constant);
        }
        this.printWriter.println(")");
    }

    @Override
    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction) {
        visitBefore(offset);
        this.printWriter.print("        ." + getName(variableInstruction));
        this.printWriter.print("(");
        if (variableInstruction.variableIndex > 3) {
            this.printWriter.print(variableInstruction.variableIndex);
        }
        if (variableInstruction.opcode == Instruction.OP_IINC)
        {
            this.printWriter.print(", ");
            this.printWriter.print(variableInstruction.constant);
        }
        this.printWriter.println(")");
    }

    @Override
    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction) {
        visitBefore(offset);
        constantArgumentFinder.reset();
        clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantArgumentFinder);
        this.printWriter.print("        .");
        String name = getName(constantInstruction);
        this.printWriter.print(name);
        if (constantArgumentFinder.isClassString && name.startsWith("ldc")) this.printWriter.print("_");
        this.printWriter.print("(");
        this.printWriter.print(constantArgumentFinder.argument);
        this.printWriter.println(")");
    }

    @Override
    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction) {
        visitBefore(offset);
        this.printWriter.println("        ." + getName(branchInstruction) + "(" + this.labelPrinter.getLabelName(offset + branchInstruction.branchOffset) + ")");
    }

    private String getName(Instruction instruction) {
        switch (instruction.opcode) {
            case Instruction.OP_RET:
            case Instruction.OP_RETURN:
            case Instruction.OP_GOTO:
            case Instruction.OP_GOTO_W:
            case Instruction.OP_NEW:
            case Instruction.OP_INSTANCEOF:
                return instruction.getName() + "_";
            case Instruction.OP_IFACMPEQ:
            case Instruction.OP_IFACMPNE:
            case Instruction.OP_IFICMPEQ:
            case Instruction.OP_IFICMPGE:
            case Instruction.OP_IFICMPGT:
            case Instruction.OP_IFICMPLE:
            case Instruction.OP_IFICMPLT:
            case Instruction.OP_IFICMPNE:
                return instruction.getName().replace("_", "");
            default:
                return instruction.getName();
        }
    }

    @Override
    public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction) {
        visitBefore(offset);
    }

    @Override
    public void visitTableSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, TableSwitchInstruction tableSwitchInstruction) {
        visitBefore(offset);
    }

    @Override
    public void visitLookUpSwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LookUpSwitchInstruction lookUpSwitchInstruction) {
        visitBefore(offset);
    }

    private static class ConstantArgumentFinder implements ConstantVisitor {
        private boolean isClassString;
        private String  argument;

        public void reset()
        {
            isClassString = false;
            argument = "";
        }

        @Override
        public void visitAnyConstant(Clazz clazz, Constant constant)
        {
            argument = "\"" + constant + "\"";
        }

        @Override
        public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant) {
            argument = String.format("\"%s\", \"%s\", \"%s\"", fieldrefConstant.getClassName(clazz), fieldrefConstant.getName(clazz), fieldrefConstant.getType(clazz));
        }

        @Override
        public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
            if (classConstant.getName(clazz).equals("Container")) {
                argument = "targetClass";
            } else {
                isClassString = true;
                argument = String.format("constantPoolEditor.addClassConstant(\"%s\", null)", classConstant.getName(clazz));
            }
        }

        @Override
        public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant) {
            argument = String.format("\"%s\", \"%s\", \"%s\"", anyMethodrefConstant.getClassName(clazz), anyMethodrefConstant.getName(clazz), anyMethodrefConstant.getType(clazz));
        }

        @Override
        public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant) {
            argument = String.format("%d, \"%s\", \"%s\"", invokeDynamicConstant.getBootstrapMethodAttributeIndex(), invokeDynamicConstant.getName(clazz), invokeDynamicConstant.getType(clazz));
        }

        @Override
        public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
            String string = stringConstant.getString(clazz)
                    .replace("\\", "\\\\")
                    .replace("\t", "\\t")
                    .replace("\b", "\\b")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\f", "\\f")
                    .replace("\"", "\\\"");
            argument = "\"" + string + "\"";
        }

        @Override
        public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
            String string = utf8Constant.getString()
                    .replace("\\", "\\\\")
                    .replace("\t", "\\t")
                    .replace("\b", "\\b")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\f", "\\f")
                    .replace("\"", "\\\"");
            argument = "\"" + string + "\"";
        }

        @Override
        public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant) {
            argument = Integer.toString(integerConstant.getValue());
        }

        @Override
        public void visitLongConstant(Clazz clazz, LongConstant longConstant) {
            argument = Long.toString(longConstant.getValue());
        }

        @Override
        public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant) {
            argument = Float.toString(floatConstant.getValue());
        }

        @Override
        public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant) {
            argument = Double.toString(doubleConstant.getValue());
        }
    }
}
