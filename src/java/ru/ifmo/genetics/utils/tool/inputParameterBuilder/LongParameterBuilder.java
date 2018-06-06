package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

public class LongParameterBuilder extends ParameterBuilder<Long> {

    public LongParameterBuilder(@NotNull String name) {
        super(Long.class, name);
    }
}
