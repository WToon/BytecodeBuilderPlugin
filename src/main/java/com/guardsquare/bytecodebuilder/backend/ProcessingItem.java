package com.guardsquare.bytecodebuilder.backend;

import proguard.classfile.instruction.Instruction;

public class ProcessingItem {
    public enum Type {
        INSTRUCTION,
        LABEL,
        CATCH
    }

    public ProcessingItem(String labelName) {
        this.labelName = labelName;
        type = Type.LABEL;
    }

    public ProcessingItem(Instruction instruction, int offset) {
        this.instruction = instruction;
        this.instructionOffset = offset;
        type = Type.INSTRUCTION;
    }

    public ProcessingItem(CatchSpec catchSpec) {
        this.catchSpec = catchSpec;
        type = Type.CATCH;
    }

    public final Type type;
    public Instruction instruction;
    public int instructionOffset;
    public String labelName;
    public CatchSpec catchSpec;
}
