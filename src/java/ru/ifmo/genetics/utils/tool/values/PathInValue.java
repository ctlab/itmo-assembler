package ru.ifmo.genetics.utils.tool.values;

import java.io.File;

/**
 * Class for storing path and for simple manipulating with it
 */
public class PathInValue implements InValue<File> {
    private InValue<File> parent;
    private InValue<String> child;


    public PathInValue(String fileName) {
        set(fileName);
    }
    public PathInValue(InValue<File> fileName) {
        set(fileName);
    }
    public PathInValue(InValue<File> parent, String child) {
        set(parent, child);
    }
    public PathInValue(InValue<File> parent, InValue<String> child) {
        set(parent, child);
    }


    public void set(String file) {
        set(new FileInValue(file));
    }
    public void set(InValue<File> parent, String child) {
        set(parent, new SimpleInValue<String>(child));
    }
    public void set(InValue<File> parent, InValue<String> child) {
        this.parent = parent;
        this.child = child;
    }
    public void set(InValue<File> file) {
        set(file, (InValue<String>) null);
    }

    

    @Override
    public File get() {
        if (child == null) {
            return parent.get();
        }
        return new File(parent.get(), child.get());
    }


    public PathInValue append(String child) {
        return new PathInValue(this, child);
    }

    @Override
    public String toString() {
        return get().getPath();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PathInValue) {
            return get().getAbsolutePath().equals(((PathInValue) obj).get().getAbsolutePath());
        }
        return false;
    }
}
