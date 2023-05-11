package com.guardsquare.bytecodebuilder.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionLabelManager
{
    private final List<String> labelNames = new ArrayList<>();
    int tryStartLabels = 0;
    int tryEndLabels   = 0;
    int handlerLabels  = 0;

    private String registerFresh(String formatString, int number)
    {
        String result = String.format(formatString, number);
        labelNames.add(result);
        return result;
    }

    public String getFreshTryStartLabel()
    {
        return registerFresh("TRY_START_%s", tryStartLabels++);
    }

    public String getFreshTryEndLabel()
    {
        return registerFresh("TRY_END_%s", tryEndLabels++);
    }

    public String getFreshHandlerLabel()
    {
        return registerFresh("HANDLER_START_%s", handlerLabels++);
    }

    public List<String> getLabelCreationStatements() {
        return labelNames.stream()
                .map(labelName -> String.format("Label %s = composer.createLabel();", labelName))
                .collect(Collectors.toList());
    }
}
