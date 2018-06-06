package ru.ifmo.genetics.statistics.reporter;

public class IgnoringReporter<R extends Report> implements Reporter<R> {
    @Override
    public void mergeFrom(R smallReport) {
    }
}
