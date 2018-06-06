package ru.ifmo.genetics.tools.rf.task;

import ru.ifmo.genetics.statistics.reporter.Counter;
import ru.ifmo.genetics.statistics.reporter.Report;

public class FillingReport extends Report {
    public Counter ok = addCounter("ok");
    public Counter notFound = addCounter("not found");
    public Counter noAnchor = addCounter("no anchor");
    public Counter ambiguous = addCounter("ambigous");
    public Counter tooBig = addCounter("too big");
    public Counter tooShort = addCounter("too short");
    public Counter tooPolymorphic = addCounter("too polymorphic");
    public Counter dropped = addCounter("dropped");
    public Counter processed = addCounter("processed");
    public Counter multipleOrientation = addCounter("multiple orientation");

    public FillingReport() {
        super("Filling statistics");
        setLayout(
                ok.fraction(processed),
                notFound.fraction(processed),
                noAnchor.fraction(processed),
                ambiguous.fraction(processed),
                tooBig.fraction(processed),
                tooShort.fraction(processed),
                tooPolymorphic.fraction(processed),
                dropped,
                processed,
                multipleOrientation
        );
    }

    public double okPercent() {
        return (double)ok.value() / processed.value();
    }
}
