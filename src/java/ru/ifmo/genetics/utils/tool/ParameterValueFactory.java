package ru.ifmo.genetics.utils.tool;

import java.util.Collection;

public interface ParameterValueFactory<T> {
    public T get(String s);
    public Collection<String> options();
}
