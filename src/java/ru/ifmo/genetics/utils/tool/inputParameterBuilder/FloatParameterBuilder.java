package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

public class FloatParameterBuilder extends ParameterBuilder<Float> {

    public FloatParameterBuilder(@NotNull String name) {
        super(Float.class, name);
    }
}
