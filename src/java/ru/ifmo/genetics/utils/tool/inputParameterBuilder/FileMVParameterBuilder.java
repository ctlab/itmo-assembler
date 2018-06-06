package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FileMVParameterBuilder extends MultiValuedParameterBuilder<File> {

    public FileMVParameterBuilder(@NotNull String name) {
        super(File[].class, name);
    }
}
