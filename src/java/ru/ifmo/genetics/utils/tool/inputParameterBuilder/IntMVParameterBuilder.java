package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

public class IntMVParameterBuilder extends MultiValuedParameterBuilder<Integer> {

    public IntMVParameterBuilder(@NotNull String name) {
        super(Integer[].class, name);
    }
}
