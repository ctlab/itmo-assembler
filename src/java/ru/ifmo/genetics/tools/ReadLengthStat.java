package ru.ifmo.genetics.tools;

import org.apache.commons.lang.mutable.MutableInt;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.io.ReadersUtils;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.tool.ExecutionFailedException;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.FileMVParameterBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class ReadLengthStat extends Tool {
        public static final String NAME = "read-length-stat";
        public static final String DESCRIPTION = "prints read length distribution";

        public final Parameter<File[]> readFiles = addParameter(new FileMVParameterBuilder("read-files")
                .mandatory()
                .withShortOpt("i")
                .withDescription("files with paired reads")
                .create());


    @Override
    protected void cleanImpl() {
    }

    @Override
    protected void runImpl() throws IOException {
        TreeMap<Integer, MutableInt> stat = new TreeMap<Integer, MutableInt>();
        for (File f: readFiles.get()) {
            NamedSource<Dna> reader = ReadersUtils.readDnaLazy(f);
            for (Dna d: reader) {
                Misc.addMutableInt(stat, d.length(), 1);
            }
        }
        for (Map.Entry<Integer, MutableInt> e: stat.entrySet()) {
            System.out.println(e.getKey() + " " + e.getValue());
        }
    }

    public static void main(String[] args) {
        new ReadLengthStat().mainImpl(args);
    }

    public ReadLengthStat() {
        super(NAME, DESCRIPTION);
    }
}
