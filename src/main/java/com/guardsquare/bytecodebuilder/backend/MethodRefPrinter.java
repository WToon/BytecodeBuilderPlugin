package com.guardsquare.bytecodebuilder.backend;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.InvokeDynamicConstant;
import proguard.classfile.constant.MethodHandleConstant;
import proguard.classfile.constant.MethodrefConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.BranchTargetFinder;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MethodRefPrinter implements InstructionVisitor, ConstantVisitor {
    private final PrintWriter printWriter;

    public MethodRefPrinter(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    @Override
    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}

    @Override
    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction) {
        if (constantInstruction.opcode == Instruction.OP_INVOKEDYNAMIC) {
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);
        }
    }

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    @Override
    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant) {
        invokeDynamicConstant.bootstrapMethodHandleAccept(clazz, new ConstantVisitor() {
            @Override
            public void visitAnyConstant(Clazz clazz, Constant constant) {
                System.out.println("HEll");
            }

            @Override
            public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant) {

                clazz.constantPoolEntryAccept(methodHandleConstant.u2referenceIndex, new ConstantVisitor() {
                    @Override
                    public void visitAnyConstant(Clazz clazz, Constant constant) {}

                    @Override
                    public void visitMethodrefConstant(Clazz clazz, MethodrefConstant methodrefConstant) {
                        System.out.println(methodrefConstant.getName(clazz));
                    }
                });
            }
        });
    }
}
