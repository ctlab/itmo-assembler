package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

public class IntParameterBuilder extends ParameterBuilder<Integer> {

    public IntParameterBuilder(@NotNull String name) {
        super(Integer.class, name);
    }
}
