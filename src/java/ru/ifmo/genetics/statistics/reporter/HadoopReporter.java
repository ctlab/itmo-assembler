package ru.ifmo.genetics.statistics.reporter;


public class HadoopReporter<R extends Report> implements Reporter<R> {
    private org.apache.hadoop.mapred.Reporter internalReporter;

    public HadoopReporter(org.apache.hadoop.mapred.Reporter internalReporter) {
        this.internalReporter = internalReporter;
    }

    @Override
    public void mergeFrom(R smallReport) {
        for (Counter c: smallReport.counters) {
            internalReporter.incrCounter(smallReport.name(), c.name, c.value());
        }
    }
}
