package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

public class StringParameterBuilder extends ParameterBuilder<String> {

    public StringParameterBuilder(@NotNull String name) {
        super(String.class, name);
    }
}
