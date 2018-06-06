package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

public class StringMVParameterBuilder extends MultiValuedParameterBuilder<String> {

    public StringMVParameterBuilder(@NotNull String name) {
        super(String[].class, name);
    }
}
