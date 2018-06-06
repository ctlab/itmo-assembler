package ru.ifmo.genetics.tools.microassembly;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.tools.io.FilesMerger;
import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileParameterBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class HolesToOverlapsConverter extends Tool {
    public static final String NAME = "holes2overlaps";
    public static final String DESCRIPTION = "converts holes to overlaps";


    // input parameters
    public final Parameter<File> holesDir = addParameter(new FileParameterBuilder("holes-dir")
            .mandatory()
            .withDescription("holes directory")
            .create());

    public final Parameter<File> allContigsFile = addParameter(new FileParameterBuilder("all-contigs-files")
            .mandatory()
            .withDescription("files with all contigs")
            .create());

    public final Parameter<File> resultingHolesFile = addParameter(new FileParameterBuilder("holes-file")
            .withDefaultValue(workDir.append("holes"))
            .withDescription("file with all holes")
            .create());

    public final Parameter<File> resultingOverlapsFile = addParameter(new FileParameterBuilder("overlaps-file")
            .withDefaultValue(workDir.append("holes.overlaps"))
            .withDescription("file with all holes")
            .create());



    // internal variables

    // output parameters


    @Override
    protected void runImpl() throws ExecutionFailedException {
        try {
            info("Merging holes to one file...");
            FilesMerger merger = new FilesMerger();
            merger.files.set(holesDir.get().listFiles());
            merger.resultingFile.set(resultingHolesFile);
            merger.simpleRun();

            holesToOverlaps();

        } catch (IOException e) {
            throw new ExecutionFailedException(e);
        }
    }

    void holesToOverlaps() throws IOException {
        info("Converting holes to overlaps...");

        ArrayList<Dna> reads = ReadersUtils.loadDnasAndAddRC(allContigsFile.get());

        Overlaps<Dna> overlaps = new Overlaps<Dna>(reads, availableProcessors.get(), true);
        Text line = new Text();

        LineReader holesReader = new LineReader(new BufferedInputStream(new FileInputStream(resultingHolesFile.get())));

        while (holesReader.readLine(line) != 0) {
            String s = line.toString();
            FilledHole fh = new FilledHole(s);

            if (fh.hole.isOpen()) {
                continue;
            }

            int iFrom = fh.hole.leftContigId * 2 + (fh.hole.leftComplemented ? 1 : 0);
            int iTo = fh.hole.rightContigId * 2 + (fh.hole.rightComplemented ? 1 : 0);
            int shift = reads.get(iFrom).length() + fh.filler.distance;
            overlaps.addRawOverlap(iFrom, iTo,
                    shift, fh.filler.weight);
        }
        holesReader.close();

        overlaps.printToFile(resultingOverlapsFile.get());
    }
    

    @Override
    protected void cleanImpl() {
    }

    public HolesToOverlapsConverter() {
        super(NAME, DESCRIPTION);
    }

    public static void main(String[] args) {
        new HolesToOverlapsConverter().mainImpl(args);
    }
}
