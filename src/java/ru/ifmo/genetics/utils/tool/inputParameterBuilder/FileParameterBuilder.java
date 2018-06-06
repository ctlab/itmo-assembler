package ru.ifmo.genetics.utils.tool.inputParameterBuilder;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.utils.tool.values.FileInValue;

import java.io.File;

public class FileParameterBuilder extends ParameterBuilder<File> {

    public FileParameterBuilder(@NotNull String name) {
        super(File.class, name);
    }
}
