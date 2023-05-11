package com.guardsquare.bytecodebuilder.backend;

public class CatchSpec {
    public String tryStartLabel;
    public String tryEndLabel;
    public String handlerLabel;
    public String exceptionClassName;

    public CatchSpec(String tryStartLabel, String tryEndLabel, String handlerLabel, String exceptionClassName) {
        this.tryStartLabel = tryStartLabel;
        this.tryEndLabel = tryEndLabel;
        this.handlerLabel = handlerLabel;
        this.exceptionClassName = exceptionClassName;
    }
}
