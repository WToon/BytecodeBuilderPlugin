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

public class InstructionPrinter implements InstructionVisitor, ConstantVisitor {
    private final PrintWriter printWriter;
    private final BranchTargetFinder targetFinder;
    private final LabelPrinter labelPrinter;

    public InstructionPrinter(PrintWriter printWriter, BranchTargetFinder targetFinder, LabelPrinter labelPrinter) {
        this.printWriter = printWriter;
        this.targetFinder = targetFinder;
        this.labelPrinter = labelPrinter;
    }

    private void visitBefore(int offset) {
        if (this.targetFinder.isBranchTarget(offset)) {
            printWriter.println("\t.label(" + this.labelPrinter.getLabelName(offset) + ")");
        }
    }

    @Override
    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction) {
        visitBefore(offset);
        this.printWriter.print("\t." + getName(simpleInstruction));
        this.printWriter.print("(");
        if (simpleInstruction.constant > 5) {
            this.printWriter.print(simpleInstruction.constant);
        }
        this.printWriter.println(")");
    }

    @Override
    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction) {
        visitBefore(offset);
        this.printWriter.print("\t." + getName(variableInstruction));
        this.printWriter.print("(");
        if (variableInstruction.variableIndex > 3) {
            this.printWriter.print(variableInstruction.variableIndex);
        }
        this.printWriter.println(")");
    }

    @Override
    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction) {
        visitBefore(offset);
        this.printWriter.print("\t.");
        this.printWriter.print(getName(constantInstruction));
        this.printWriter.print("(");
        clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);
        this.printWriter.println(")");
    }

    @Override
    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction) {
        visitBefore(offset);
        this.printWriter.println("\t." + getName(branchInstruction) + "(" + this.labelPrinter.getLabelName(offset + branchInstruction.branchOffset) + ")");
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

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {
        this.printWriter.print("\"" + constant + "\"");
    }

    @Override
    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant) {
        this.printWriter.printf("\"%s\", \"%s\", \"%s\"", fieldrefConstant.getClassName(clazz), fieldrefConstant.getName(clazz), fieldrefConstant.getType(clazz));
    }

    @Override
    public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
        this.printWriter.print("\"" + classConstant.getName(clazz) + "\"");
    }

    @Override
    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant) {
        this.printWriter.printf("\"%s\", \"%s\", \"%s\"", anyMethodrefConstant.getClassName(clazz), anyMethodrefConstant.getName(clazz), anyMethodrefConstant.getType(clazz));
    }

    @Override
    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant) {
        this.printWriter.printf("%d, \"%s\", \"%s\"", invokeDynamicConstant.getBootstrapMethodAttributeIndex(), invokeDynamicConstant.getName(clazz), invokeDynamicConstant.getType(clazz));
    }

    @Override
    public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
        this.printWriter.print("\"" + stringConstant.getString(clazz) + "\"");
    }

    @Override
    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
        this.printWriter.print("\"" + utf8Constant.getString() + "\"");
    }

    @Override
    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant) {
        this.printWriter.print(integerConstant.getValue());
    }

    @Override
    public void visitLongConstant(Clazz clazz, LongConstant longConstant) {
        this.printWriter.print(longConstant.getValue());
    }

    @Override
    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant) {
        this.printWriter.print(floatConstant.getValue());
    }

    @Override
    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant) {
        this.printWriter.print(doubleConstant.getValue());
    }
}
