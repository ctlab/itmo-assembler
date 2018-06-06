package ru.ifmo.genetics.io;

import org.apache.log4j.Logger;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaQ;
import ru.ifmo.genetics.io.formats.IllegalQualityValueException;
import ru.ifmo.genetics.io.formats.Illumina;
import ru.ifmo.genetics.io.formats.QualityFormat;
import ru.ifmo.genetics.io.formats.Sanger;
import ru.ifmo.genetics.io.readers.*;
import ru.ifmo.genetics.io.sources.ConsecutiveSource;
import ru.ifmo.genetics.io.sources.NamedSource;
import ru.ifmo.genetics.io.sources.Source;
import ru.ifmo.genetics.io.readers.DnaQReaderFromDnaSource;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import static ru.ifmo.genetics.utils.FileUtils.removeExtension;

public class ReadersUtils {

    private static Logger logger = Logger.getLogger("readers-utils");

    public static String detectFileFormat(File file) throws IOException {
        if (file == null) {
            return null;
        }

        String fileName = file.getName().toLowerCase();

        String suffix = "";
        if (fileName.endsWith(".gz")) {
            suffix = ".gz";
            fileName = removeExtension(fileName, ".gz");
        }
        if (fileName.endsWith(".bz2")) {
            suffix = ".bz2";
            fileName = removeExtension(fileName, ".bz2");
        }


        if (fileName.endsWith(".binq")) {
            return "binq" + suffix;
        } else if (fileName.endsWith(".fastq") || fileName.endsWith(".fq")) {
            return "fastq" + suffix;
        } else if (fileName.endsWith(".fasta") || fileName.endsWith(".fa") || fileName.endsWith(".fn")
                || fileName.endsWith(".fna")) {
            return "fasta" + suffix;
        }
        throw new IOException("Can't detect file format for file " + fileName);
    }


    public static int DEFAULT_READS_NUMBER_TO_DETERMINE_QUALITY_FORMAT = 1000;

    public static QualityFormat determineQualityFormat(File file) throws IOException {
        return determineQualityFormat(file, DEFAULT_READS_NUMBER_TO_DETERMINE_QUALITY_FORMAT);
    }

    public static QualityFormat determineQualityFormat(File file, int head) throws IOException {
        QualityFormat ans = Illumina.instance;
        try {
            Source<DnaQ> reader = readDnaQLazy(file, ans);
            Iterator<DnaQ> it = reader.iterator();
            for (int i = 0; (i < head) && it.hasNext(); i++) {
                it.next();
            }
            // OK
        } catch (IllegalQualityValueException e) {
            ans = Sanger.instance;
        }
        Tool.debug(logger, "Determined quality format of file " + file.getName() + " as " + ans.toExtString());
        return ans;
    }

//    ======================== Dna Readers =============================

    public static NamedSource<Dna> readDnaLazy(File file) throws IOException {
        return readDnaLazy(file, null);
    }

    public static NamedSource<Dna> readDnaLazy(File file, String fileFormat) throws IOException {
        if (fileFormat == null) {
            fileFormat = detectFileFormat(file);
        }
        fileFormat = fileFormat.toLowerCase();

        if (fileFormat.equals("fasta")) {
            return new FastaReader(file);
        } else if (fileFormat.equals("fasta.gz")) {
            return new FastaGZReader(file);
        } else if (fileFormat.equals("fasta.bz2")) {
            return new FastaBZ2Reader(file);
        } else if (fileFormat.startsWith("fastq") || fileFormat.equals("binq")) {
            return new FastaReaderFromXQSource(readDnaQLazy(file));
        } else {
            throw new RuntimeException("Illegal format " + fileFormat + ", file " + file.getName());
        }
    }

    public static ArrayList<Dna> loadDnas(File file) throws IOException {
        ArrayList<Dna> ans = new ArrayList<Dna>();

        Source<Dna> in = readDnaLazy(file);
        Iterator<Dna> it = in.iterator();
        while (it.hasNext()) {
            ans.add(it.next());
        }

        return ans;
    }

    public static ArrayList<Dna> loadDnasAndAddRC(File file) throws IOException {
        ArrayList<Dna> ans = new ArrayList<Dna>();

        Source<Dna> in = readDnaLazy(file);
        Iterator<Dna> it = in.iterator();
        while (it.hasNext()) {
            Dna dna = it.next();
            ans.add(dna);
            ans.add(dna.reverseComplement());
        }

        return ans;
    }


    public static Source<Dna> readDnaLazy(File... files) throws IOException {
        Source[] sources = new Source[files.length];
        for (int i = 0; i < files.length; i++) {
            sources[i] = readDnaLazy(files[i]);
        }

        //noinspection unchecked
        return new ConsecutiveSource<Dna>(sources);
    }

    public static ArrayList<Dna> loadDnas(File... files) throws IOException {
        ArrayList<Dna> ans = new ArrayList<Dna>();

        Source<Dna> in = readDnaLazy(files);
        Iterator<Dna> it = in.iterator();
        while (it.hasNext()) {
            ans.add(it.next());
        }

        return ans;
    }



//    ======================== DnaQ Readers =============================

    public static final int DEFAULT_PHRED_FOR_FASTA = 20;

    public static NamedSource<DnaQ> readDnaQLazy(File file) throws IOException {
        return readDnaQLazy(file, null, null, DEFAULT_PHRED_FOR_FASTA);
    }
    public static NamedSource<DnaQ> readDnaQLazy(File file, QualityFormat qualityFormat) throws IOException {
        return readDnaQLazy(file, null, qualityFormat, DEFAULT_PHRED_FOR_FASTA);
    }

    public static NamedSource<DnaQ> readDnaQLazy(File file, String fileFormat, QualityFormat qualityFormat,
                                                 int phredForFasta) throws IOException {
        if (fileFormat == null) {
            fileFormat = detectFileFormat(file);
        }
        fileFormat = fileFormat.toLowerCase();
        if (qualityFormat == null && fileFormat.startsWith("fastq")) {
            qualityFormat = determineQualityFormat(file);
        }

        if (fileFormat.equals("fastq")) {
            return new FastqReader(file, qualityFormat);
        } else if (fileFormat.equals("fastq.gz")) {
            return new FastqGZReader(file, qualityFormat);
        } else if (fileFormat.equals("fastq.bz2")) {
            return new FastqBZ2Reader(file, qualityFormat);
        } else if (fileFormat.equals("fasta")) {
            return new DnaQReaderFromDnaSource(
                    new FastaWithNsReader(file), phredForFasta);
        } else if (fileFormat.equals("fasta.gz")) {
            return new DnaQReaderFromDnaSource(
                    new FastaWithNsGZReader(file), phredForFasta);
        } else if (fileFormat.equals("fasta.bz2")) {
            return new DnaQReaderFromDnaSource(
                    new FastaWithNsBZ2Reader(file), phredForFasta);
        } else if (fileFormat.equals("binq")) {
            return new BinqReader(file);
        } else {
            throw new RuntimeException("Illegal format " + fileFormat);
        }
    }

    public static ArrayList<DnaQ> loadDnaQs(File file) throws IOException {
        ArrayList<DnaQ> ans = new ArrayList<DnaQ>();

        Source<DnaQ> in = readDnaQLazy(file);
        Iterator<DnaQ> it = in.iterator();
        while (it.hasNext()) {
            ans.add(it.next());
        }

        return ans;
    }


    public static Source<DnaQ> readDnaQLazy(File... files) throws IOException {
        Source[] sources = new Source[files.length];
        for (int i = 0; i < files.length; i++) {
            sources[i] = readDnaQLazy(files[i]);
        }

        //noinspection unchecked
        return new ConsecutiveSource<DnaQ>(sources);
    }

    public static ArrayList<DnaQ> loadDnaQs(File... files) throws IOException {
        ArrayList<DnaQ> ans = new ArrayList<DnaQ>();

        Source<DnaQ> in = readDnaQLazy(files);
        Iterator<DnaQ> it = in.iterator();
        while (it.hasNext()) {
            ans.add(it.next());
        }

        return ans;
    }
}
