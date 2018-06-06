package ru.ifmo.genetics.statistics.reporter;

public interface Reporter<T extends Report> {
    public void mergeFrom(T smallReport);
}
