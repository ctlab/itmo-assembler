package ru.ifmo.genetics.tools.io;

import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.utils.tool.values.InValue;
import ru.ifmo.genetics.utils.tool.values.Yielder;

import java.io.File;
import java.io.IOException;

public class FileFormatYielder extends Yielder<String> {

    InValue<File> file;

    public FileFormatYielder(InValue<File> file) {
        this.file = file;
    }

    @Override
    public String yield() {
        try {
            return ReadersUtils.detectFileFormat(file.get());
        } catch (IOException e) {
            throw new RuntimeException(e);    // better handling?
        }
    }

    @Override
    public String description() {
        return "determines format based on file extension";
    }
}
