package ru.ifmo.genetics.utils.tool.values;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ListFilesYielder extends Yielder<File[]> {
    private final InValue<File> directory;
    private final FileFilter filter;


    public ListFilesYielder(InValue<File> directory) {
        this(directory, (Pattern) null);
    }

    public ListFilesYielder(InValue<File> directory, String rePattern) {
        this(directory, Pattern.compile(rePattern));
    }
    public ListFilesYielder(InValue<File> directory, final Pattern pattern) {
        this.directory = directory;
        if (pattern == null) {
            filter = null;
        } else {
            filter = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pattern.matcher(pathname.toString()).matches();
                }
            };
        }
    }

    @Override
    public File[] yield() {
        File[] ar = (filter == null) ? directory.get().listFiles() : directory.get().listFiles(filter);
        Arrays.sort(ar);
        return ar;
    }
    @Override
    public String description() {
        return "gets all files in directory " + directory;
    }
}
