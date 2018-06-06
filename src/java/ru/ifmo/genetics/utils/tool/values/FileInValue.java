package ru.ifmo.genetics.utils.tool.values;

import java.io.File;

public class FileInValue implements InValue<File> {

    InValue<String> fileName;

    public FileInValue(InValue<String> fileName) {
        this.fileName = fileName;
    }

    public FileInValue(String fileName) {
        this.fileName = new SimpleInValue<String>(fileName);
    }

    @Override
    public File get() {
        return new File(fileName.get());
    }

    @Override
    public String toString() {
        return fileName.get();
    }
}
