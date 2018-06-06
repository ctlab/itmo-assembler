package ru.ifmo.genetics.statistics.reporter;

public class LocalReporter<T extends Report> implements Reporter<T> {
    private final T report;
    public LocalReporter(T initialReport) {
        this.report = initialReport;
    }

    @Override
    public void mergeFrom(T smallReport) {
        report.mergeFrom(smallReport);
    }

    @Override
    public String toString() {
        return report.toString();
    }
}
