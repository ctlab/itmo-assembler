package ru.ifmo.genetics.utils.tool.values;

import java.io.File;

public class FilesFromOneFileYielder extends Yielder<File[]> {
    private final InValue<File> file;

    public FilesFromOneFileYielder(File file) {
        this(new SimpleInValue<File>(file));
    }
    public FilesFromOneFileYielder(InValue<File> file) {
        this.file = file;
    }

    @Override
    public File[] yield() {
        return new File[]{file.get()};
    }
    @Override
    public String description() {
        return "return file from given InValue<File>";
    }
}
