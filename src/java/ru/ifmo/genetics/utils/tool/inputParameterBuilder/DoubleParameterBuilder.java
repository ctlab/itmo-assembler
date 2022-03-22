package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

public class DoubleParameterBuilder extends ParameterBuilder<Double> {

    public DoubleParameterBuilder(@NotNull String name) {
        super(Double.class, name);
    }
}
