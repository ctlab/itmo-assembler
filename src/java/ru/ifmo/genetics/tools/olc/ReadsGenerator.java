package ru.ifmo.genetics.tools.olc;

import ru.ifmo.genetics.dna.DnaTools;

import java.io.*;
import java.util.*;

public class ReadsGenerator {
    final boolean WRITE_READS_INFO_FILE = true;
    public static final String READS_INFO_FILE = "../data/readsFromGen.info";

    private static int COVERAGE;
    private static double ERRORS_PERCENT;
    private static String READS_FILE;
    private static String GENOME_FASTA_FILE;
    private static int READ_LENGTH;
    private static int READ_LENGTH_DEVIATION;

    Random r = new Random(401867410256163L);


    void run() throws IOException {
        String genome = loadFromFasta(GENOME_FASTA_FILE);
        PrintWriter out = new PrintWriter(READS_FILE);

        int readsCount = (genome.length() / READ_LENGTH) * COVERAGE;
        System.err.println("Reads count = " + readsCount);

        List<ReadInfo> readList = new ArrayList<ReadInfo>();

        for (int i = 0; i < readsCount; i++) {
            int readLen = -1;
            while (readLen <= 0) {
                readLen = READ_LENGTH + (int) (r.nextGaussian() * READ_LENGTH_DEVIATION);
            }

            int beginPos = r.nextInt(genome.length() - readLen + 1);

            String read = genome.substring(beginPos, beginPos + readLen);
            read = addDirt(read);
            boolean rc = false;
            if (r.nextDouble() < 0.5) {
                read = DnaTools.reverseComplement(read);
                rc = true;
            }

            out.println(">" + i + " | len=" + readLen + " | beginPos=" + beginPos + " | rc=" + rc);
            out.println(read);

            if (WRITE_READS_INFO_FILE) {
                readList.add(new ReadInfo(i, beginPos, readLen, rc));
            }

            if ((i & 0x10000) != 0) {
                System.err.print("\r" + (i * 100 / readsCount) + "%");
            }
        }
        out.close();

        if (WRITE_READS_INFO_FILE) {
//            String readsWD;
//            int lastS = READS_FILE.lastIndexOf(File.separator);
//            if (lastS == -1) {
//                readsWD = "";
//            } else {
//                readsWD = READS_FILE.substring(0, lastS);
//            }
//            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(readsWD, READS_INFO_FILE)));
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(READS_INFO_FILE));
            os.writeObject(readList);
            os.close();
        }

        System.err.println("\r100%");
    }

    String addDirt(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (r.nextDouble() < ERRORS_PERCENT / 100.0) {
                c[i] = DnaTools.NUCLEOTIDES[r.nextInt(DnaTools.NUCLEOTIDES.length)];
            }
        }
        return new String(c);
    }

    public static String loadFromFasta(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String s = br.readLine();
            if (s == null)
                break;
            if (s.startsWith(">"))
                continue;
            sb.append(s);
        }

        System.err.println("Genome length = " + sb.length());
        return sb.toString();
    }

    public static class ReadInfo implements Comparable<ReadInfo>, Serializable {
        private static final long serialVersionUID = 1L;

        public int no;
        public int beginPos, len;
        public boolean rc;

        ReadInfo(int no, int beginPos, int len, boolean rc) {
            this.no = no;
            this.beginPos = beginPos;
            this.len = len;
            this.rc = rc;
        }
        public ReadInfo(ReadInfo other) {
            no = other.no;
            beginPos = other.beginPos;
            len = other.len;
            rc = other.rc;
        }

        @Override
        public int compareTo(ReadInfo o) {
            return beginPos - o.beginPos;
        }

        @Override
        public String toString() {
            return "ReadInfo{" + "no = " + no +", beginPos = " + beginPos +", len = " + len + ", rc = " + rc + '}';
        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Using: java ReadsGenerator <config-file>");
            return;
        }

        Properties props = new Properties();
        FileInputStream in = new FileInputStream(args[0]);
        props.load(in);
        in.close();

        READS_FILE = props.getProperty("reads");
        GENOME_FASTA_FILE = props.getProperty("generator.genome");
        COVERAGE = Integer.parseInt(props.getProperty("generator.coverage"));
        ERRORS_PERCENT = Double.parseDouble(props.getProperty("generator.errors_percent", "0.0"));
        READ_LENGTH = Integer.parseInt(props.getProperty("generator.read_length"));
        READ_LENGTH_DEVIATION = Integer.parseInt(props.getProperty("generator.read_length_deviation"));

        new ReadsGenerator().run();
    }
}
