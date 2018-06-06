package ru.ifmo.genetics.tools.irf;

import ru.ifmo.genetics.statistics.reporter.Counter;
import ru.ifmo.genetics.statistics.reporter.Report;

public class FillingReport extends Report {
    public Counter ok = addCounter("ok");
    public Counter tooBig = addCounter("too big");
    public Counter tooShort = addCounter("too short");
    public Counter noAnchor = addCounter("no anchor");
    public Counter notFound = addCounter("not found");
    public Counter cyclic = addCounter("has cycle");
    public Counter ambiguous = addCounter("ambiguous");
    public Counter processed = addCounter("processed");

    public FillingReport() {
        super("Filling statistics");
        setLayout(
                ok.fraction(processed),
                tooBig.fraction(processed),
                tooShort.fraction(processed),
                noAnchor.fraction(processed),
                notFound.fraction(processed),
                ambiguous.fraction(processed),
                cyclic.fraction(processed),
                processed
        );
    }
}
